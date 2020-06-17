/*
 * Copyright (c) 2019. All rights reserved.
 * ClientSession.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.handler.outbound.AbstractActionPromise;
import org.typhoon.batman.client.message.MqttMessage;
import org.typhoon.batman.client.message.MqttMessageType;
import org.typhoon.batman.client.message.MqttPublishMessage;
import org.typhoon.batman.client.store.SessionStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.typhoon.batman.client.ConnectionState.*;

/**
 * @author C.
 */
public class ClientSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSession.class);

    private String clientId;
    private final ReadWriteLock connLock = new ReentrantReadWriteLock();
    private ConnectionState connectionState;
    private final HeartbeatSprite heartBeatSprite;
    private final SessionStore sessionStore;
    private final PromiseKeeper promiseKeeper;
    /*
     * @@ in-flight message zone stores publish messages sent by server with qos2
     * @@ it is no need to persistent, but we need to save it temporarily as the message notification
     * @@ is fired after pubrel.
     */
    private final Map<Integer, MqttPublishMessage> inFlightMessages = new ConcurrentHashMap<>();
    private final AtomicBoolean quiesce = new AtomicBoolean(false);
    private boolean terminated; // @@ indicate that the client is manually terminated.
    private boolean reconnecting;

    public ClientSession(SessionStore sessionStore, PromiseKeeper promiseKeeper, HeartbeatSprite heartbeatSprite) {
        this.sessionStore = sessionStore;
        this.promiseKeeper = promiseKeeper;
        this.heartBeatSprite = heartbeatSprite;
        this.connectionState = DISCONNECTED;
    }

    public void aquireStateLock() {
        connLock.writeLock().lock();
    }

    public void releaseStateLock() {
        connLock.writeLock().unlock();
    }

    public void notifyConnected() {
        updateState(CONNECTED);
        reconnecting = false;
        heartBeatSprite.start();
    }

    public void notifySent(MqttMessage message) {
        heartBeatSprite.notifySent(message);
        if (message.fixedHeader().messageType() != MqttMessageType.PINGREQ) {
            AbstractActionPromise<?> promise = (AbstractActionPromise<?>) promiseKeeper.retrieve(message);
            if (promise != null) {
                promise.notifySent();
            }
        }
    }

    public void notifySendFailed(MqttMessage message, MqttException cause) {
        if (message.fixedHeader().messageType() != MqttMessageType.PINGREQ) {
            AbstractActionPromise<?> promise = (AbstractActionPromise<?>) promiseKeeper.retrieve(message);
            if (promise != null) {
                promise.notifySendFailed(cause);
            }
        }
    }

    public void notifyReceived(MqttMessage message) {
        heartBeatSprite.notifyReceived(message);
    }

    public void updateState(ConnectionState state) {
        connLock.writeLock().lock();
        try {
            connectionState = state;
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Client: {} state changed to {}", clientId, state);
            }
            if (state == DISCONNECTING) {
                heartBeatSprite.stop();
            }
        } finally {
            connLock.writeLock().unlock();
        }
    }

    public boolean isConnected() {
        connLock.readLock().lock();
        try {
            return connectionState == CONNECTED;
        } finally {
            connLock.readLock().unlock();
        }
    }

    public boolean isConnecting() {
        connLock.readLock().lock();
        try {
            return connectionState == CONNECTING;
        } finally {
            connLock.readLock().unlock();
        }
    }

    public boolean isDisconnected() {
        connLock.readLock().lock();
        try {
            return connectionState == DISCONNECTED;
        } finally {
            connLock.readLock().unlock();
        }
    }

    public boolean isDisconnecting() {
        connLock.readLock().lock();
        try {
            return connectionState == DISCONNECTING;
        } finally {
            connLock.readLock().unlock();
        }
    }

    public boolean isClosed() {
        connLock.readLock().lock();
        try {
            return connectionState == CLOSED;
        } finally {
            connLock.readLock().unlock();
        }
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * add inbound publish messages to in-flight zone.
     *
     * @param publishMessage
     */
    public void inFlight(MqttPublishMessage publishMessage) {
        inFlightMessages.put(publishMessage.variableHeader().packetId(), publishMessage);
    }

    public MqttPublishMessage inFlightAck(int packetId) {
        return inFlightMessages.remove(packetId);
    }

    public int getInFlightCount() {
        return inFlightMessages.size();
    }

    public void quiesce(long timeout, TimeUnit timeUnit) {
        if (timeout > 0 && quiesce.compareAndSet(false, true)) {
            long cost = 0;
            long total = timeUnit.toMillis(timeout);
            while (cost < total) {
                if (!promiseKeeper.isEmpty() || sessionStore.getInFlightCount() > 0 || sessionStore.getWaitingRecCount() > 0 || getInFlightCount() > 0) {
                    try {
                        Thread.sleep(Math.min(200, total - cost));
                    } catch (InterruptedException ignore) {
                    }
                    cost += 200;
                } else {
                    break;
                }
            }
        }
    }

    public boolean isQuiescent() {
        return quiesce.get();
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isTerminated() {
        synchronized (this) {
            return terminated;
        }
    }

    public void setTerminated(boolean terminated) {
        synchronized (this) {
            this.terminated = terminated;
        }
    }

    public boolean isReconnecting() {
        synchronized (this) {
            return reconnecting;
        }
    }

    public void setReconnecting(boolean reconnecting) {
        synchronized (this) {
            this.reconnecting = reconnecting;
        }
    }
}
