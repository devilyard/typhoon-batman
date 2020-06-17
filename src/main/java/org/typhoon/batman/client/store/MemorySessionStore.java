/*
 * Copyright (c) 2019. All rights reserved.
 * MemorySessionStore.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.store;

import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.OfflineOptions;
import org.typhoon.batman.client.message.MqttMessage;
import org.typhoon.batman.client.message.MqttPublishMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.typhoon.batman.Constants.MAX_PACKET_ID;
import static org.typhoon.batman.Constants.MIN_PACKET_ID;

/**
 * @author C.
 */
public class MemorySessionStore implements SessionStore {

    private Map<Integer, MqttPublishMessage> messageStore;

    private List<MqttMessage> outboundQueue;
    private final Lock outboundQueueLock = new ReentrantLock();
    private final Condition outboundQueueWaiter = outboundQueueLock.newCondition();

    private List<MqttMessage> offlineQueue;
    private final Lock offlineQueueLock = new ReentrantLock();
    private OfflineOptions offlineOptions;

    private final List<Integer> inFlightQueue = new ArrayList<>();
    private final Lock inFlightLock = new ReentrantLock();
    private int maxInFlight;

    private final List<Integer> waitingRecQueue = new ArrayList<>();
    private final Lock recLock = new ReentrantLock();

    private final AtomicInteger nextPacketId = new AtomicInteger(MIN_PACKET_ID - 1);
    private final Map<Integer, Integer> inUsePacketIds = new ConcurrentHashMap<>();

    @Override
    public void open(String clientId, String serverURI) {
        messageStore = new ConcurrentHashMap<>();
        outboundQueue = new ArrayList<>(128);
        offlineQueue = new ArrayList<>(128);
    }

    @Override
    public void close() {

    }

    @Override
    public void clear() {
        outboundQueueLock.lock();
        try {
            outboundQueue.clear();
        } finally {
            outboundQueueLock.unlock();
        }
        offlineQueueLock.lock();
        try {
            offlineQueue.clear();
        } finally {
            offlineQueueLock.unlock();
        }
        inFlightLock.lock();
        try {
            inFlightQueue.clear();
        } finally {
            inFlightLock.unlock();
        }
        recLock.lock();
        try {
            waitingRecQueue.clear();
        } finally {
            recLock.unlock();
        }
    }

    @Override
    public void store(MqttPublishMessage message) {
        messageStore.put(message.variableHeader().packetId(), message);
    }

    @Override
    public MqttPublishMessage retrieve(int packetId) {
        return messageStore.get(packetId);
    }

    @Override
    public MqttPublishMessage remove(int packetId) {
        return messageStore.remove(packetId);
    }

    @Override
    public Set<MqttPublishMessage> getAllMessages() {
        return new HashSet<>(messageStore.values());
    }

    @Override
    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    @Override
    public void setOfflineOptions(OfflineOptions offlineOptions) {
        this.offlineOptions = offlineOptions;
    }

    @Override
    public void appendToOutboundZone(MqttMessage message) {
        outboundQueueLock.lock();
        try {
            outboundQueue.add(message);
            outboundQueueWaiter.signalAll();
        } finally {
            outboundQueueLock.unlock();
        }
    }

    @Override
    public MqttMessage popFromOutboundZone() {
        outboundQueueLock.lock();
        try {
            while (outboundQueue.isEmpty() && !Thread.interrupted()) {
                outboundQueueWaiter.await(100, TimeUnit.MILLISECONDS);
            }
            return outboundQueue.remove(0);
        } catch (InterruptedException e) {
            return null;
        } finally {
            outboundQueueLock.unlock();
        }
    }

    @Override
    public void prependToOutboundZone(MqttMessage message) {
        outboundQueueLock.lock();
        try {
            outboundQueue.add(0, message);
            outboundQueueWaiter.signalAll();
        } finally {
            outboundQueueLock.unlock();
        }
    }

    @Override
    public void appendToOfflineZone(MqttMessage message) throws MqttException {
        offlineQueueLock.lock();
        try {
            if (offlineQueue.size() >= offlineOptions.getMaxBufferSize()) {
                if (offlineOptions.isBearMode()) {
                    offlineQueue.remove(0);
                } else {
                    throw new MqttException(MqttException.REASON_CODE_OFFLINE_BUFFER_FULL);
                }
            }
            offlineQueue.add(message);
        } finally {
            offlineQueueLock.unlock();
        }
    }

    @Override
    public MqttMessage popFromOfflineZone() {
        return offlineQueue.isEmpty() ? null : offlineQueue.remove(0);
    }

    @Override
    public int getNextPacketId() throws MqttException {
        int startingMessageId = nextPacketId.get();
        int loopCount = 0;
        int packetId;
        do {
            packetId = nextPacketId.incrementAndGet();
            if (packetId > MAX_PACKET_ID) {
                nextPacketId.set(MIN_PACKET_ID);
                packetId = MIN_PACKET_ID;
            }
            if (packetId == startingMessageId) {
                loopCount++;
                if (loopCount == 2) {
                    throw new MqttException(MqttException.REASON_CODE_NO_PACKET_IDS_AVAILABLE);
                }
            }
        } while (inUsePacketIds.containsKey(packetId));
        inUsePacketIds.put(packetId, packetId);
        return packetId;
    }

    @Override
    public void releasePacketId(int packetId) {
        inUsePacketIds.remove(packetId);
    }

    @Override
    public void inFlight(int packetId) throws MqttException {
        inFlightLock.lock();
        try {
            if (inFlightQueue.size() >= maxInFlight) {
                throw new MqttException(MqttException.REASON_CODE_MAX_INFLIGHT);
            }
            inFlightQueue.add(packetId);
        } finally {
            inFlightLock.unlock();
        }
    }

    @Override
    public boolean inFlightAck(int packetId) {
        inFlightLock.lock();
        try {
            inUsePacketIds.remove(packetId);
            return inFlightQueue.remove((Integer) packetId);
        } finally {
            inFlightLock.unlock();
        }
    }

    @Override
    public int getInFlightCount() {
        inFlightLock.lock();
        try {
            return inFlightQueue.size();
        } finally {
            inFlightLock.unlock();
        }
    }

    @Override
    public Set<Integer> getInFlightPacketIds() {
        inFlightLock.lock();
        try {
            return new HashSet<>(inFlightQueue);
        } finally {
            inFlightLock.unlock();
        }
    }

    @Override
    public void waitingForRec(int packetId) {
        recLock.lock();
        try {
            waitingRecQueue.add(packetId);
        } finally {
            recLock.unlock();
        }
    }

    @Override
    public void recReceived(int packetId) throws MqttException {
        recLock.lock();
        try {
            waitingRecQueue.remove((Integer) packetId);
        } finally {
            recLock.unlock();
        }
        while (getInFlightCount() >= maxInFlight && !Thread.interrupted()) {
            // @@ there must be too many message, rest for a while
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException ignore) {
            }
        }
        inFlight(packetId);
    }

    @Override
    public int getWaitingRecCount() {
        recLock.lock();
        try {
            return waitingRecQueue.size();
        } finally {
            recLock.unlock();
        }
    }

    @Override
    public Set<Integer> getWaitingRecPacketIds() {
        recLock.lock();
        try {
            return new HashSet<>(waitingRecQueue);
        } finally {
            recLock.unlock();
        }
    }
}
