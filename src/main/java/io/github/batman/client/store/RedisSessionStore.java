/*
 * Copyright (c) 2019. All rights reserved.
 * RedisSessionStore.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.store;

import io.github.batman.client.MqttException;
import io.github.batman.client.OfflineOptions;
import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttPublishMessage;
import io.github.batman.client.network.MqttDecoder;
import io.github.batman.client.network.MqttEncoder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.batman.Constants.MAX_PACKET_ID;
import static io.github.batman.Constants.MIN_PACKET_ID;

/**
 * @author C.
 */
public class RedisSessionStore implements SessionStore {

    private static final String STORE_PREFIX = "btm:session:";

    private List<MqttMessage> outboundQueue;
    private final Lock outboundQueueLock = new ReentrantLock();
    private final Condition outboundQueueWaiter = outboundQueueLock.newCondition();

    private int maxInFlight;
    private OfflineOptions offlineOptions;
    private final JedisPool jedisPool;

    private final AtomicInteger nextPacketId = new AtomicInteger(MIN_PACKET_ID - 1);
    private final Set<Integer> inUsePacketIds = new HashSet<>();
    private byte[] messageStoreKey;
    private byte[] offlineZoneKey;
    private String inUsePacketIdsKey;
    private String inFlightZoneKey;
    private String waitingRecZoneKey;

    //    @formatter:off
    private static final byte[] APPEND_TO_OFFLINE_ZONE =
            ("local c = redis.call('llen', KEYS[1]);" +
            "if c >= tonumber(ARGV[1]) then" +
            "   if ARGV[2] == 1 then" +
            "       redis.call('rpop', KEYS[1]);" +
            "       redis.call('lpush', KEYS[1], ARGV[3]);" +
            "       return 1;" +
            "   else" +
            "       return 0;" +
            "   end;" +
            "else" +
            "   redis.call('lpush', KEYS[1], ARGV[3]);" +
            "   return 1;" +
            "end;").getBytes(StandardCharsets.UTF_8);
    private static final String IN_FLIGHT =
            "local c = redis.call('scard', KEYS[1]);" +
            "if c >= tonumber(ARGV[1]) then" +
            "   return 0;" +
            "else" +
            "   redis.call('sadd', KEYS[1], ARGV[2]);" +
            "   return 1;" +
            "end;";
    private static final String IN_FLIGHT_ACK = "redis.call('srem', KEYS[1], ARGV[1]);return redis.call('srem', KEYS[2], ARGV[1]);";
    //    @formatter:on

    public RedisSessionStore(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void open(String serverURI, String clientId) throws MqttException {
        URI uri;
        try {
            uri = new URI(serverURI);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(serverURI);
        }
        String path = uri.getPath();
        messageStoreKey = (STORE_PREFIX + "msg:" + path + ":" + clientId).getBytes(StandardCharsets.UTF_8);
        offlineZoneKey = (STORE_PREFIX + "oflz:" + path + ":" + clientId).getBytes(StandardCharsets.UTF_8);
        inUsePacketIdsKey = STORE_PREFIX + "pid:" + path + ":" + clientId;
        inFlightZoneKey = STORE_PREFIX + "if:" + path + ":" + clientId;
        waitingRecZoneKey = STORE_PREFIX + "rec:" + path + ":" + clientId;
        outboundQueue = new ArrayList<>(128);

        Set<byte[]> ids;
        try (Jedis jedis = jedisPool.getResource()) {
            ids = jedis.hkeys(messageStoreKey);
            jedis.del(inUsePacketIdsKey);
            jedis.del(offlineZoneKey);// @@ publish messages will be republish from message store
        } catch (Throwable throwable) {
            throw new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR, throwable);
        }
        if (ids != null && !ids.isEmpty()) {
            int max = 0;
            String[] packetIds = new String[ids.size()];
            int i = 0;
            for (byte[] b : ids) {
                int id = PacketIdSerializer.deserialize(b);
                packetIds[i++] = String.valueOf(id);
                inUsePacketIds.add(id);
                max = Math.max(id, max);

            }
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.sadd(inUsePacketIdsKey, packetIds);
            }
            nextPacketId.set(max);
        }
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
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(offlineZoneKey);
        }
    }

    @Override
    public void store(MqttPublishMessage message) {
        byte[] packetId = PacketIdSerializer.serialize(message.variableHeader().packetId());
        byte[] data = MqttEncoder.encode(message);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(messageStoreKey, packetId, data);
        }
    }

    @Override
    public MqttPublishMessage retrieve(int packetId) {
        byte[] pid = PacketIdSerializer.serialize(packetId);
        byte[] data;
        try (Jedis jedis = jedisPool.getResource()) {
            data = jedis.hget(messageStoreKey, pid);
        }
        return data == null ? null : (MqttPublishMessage) MqttDecoder.decode(data);
    }

    @Override
    public MqttPublishMessage remove(int packetId) {
        byte[] pid = PacketIdSerializer.serialize(packetId);
        String script = "local d = redis.call('hget', KEYS[1], ARGV[1]);" +
                "if d then" +
                "   redis.call('hdel', KEYS[1], ARGV[1]);" +
                "end;" +
                "return d;";
        byte[] data;
        try (Jedis jedis = jedisPool.getResource()) {
            data = (byte[]) jedis.eval(script.getBytes(StandardCharsets.UTF_8), 1, messageStoreKey, pid);
        }
        return data == null ? null : (MqttPublishMessage) MqttDecoder.decode(data);
    }

    @Override
    public Set<MqttPublishMessage> getAllMessages() {
        List<byte[]> datas;
        try (Jedis jedis = jedisPool.getResource()) {
            datas = jedis.hvals(messageStoreKey);
        }
        if (datas != null && !datas.isEmpty()) {
            Set<MqttPublishMessage> messages = new HashSet<>(datas.size());
            for (byte[] data : datas) {
                messages.add((MqttPublishMessage) MqttDecoder.decode(data));
            }
            return messages;
        }
        return null;
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
    public void appendToOfflineZone(MqttMessage message) throws MqttException {
        byte[] data = MqttEncoder.encode(message);
        byte[] bearMode = (offlineOptions.isBearMode() ? "1" : "0").getBytes();
        try (Jedis jedis = jedisPool.getResource()) {
            long rev = (long) jedis.eval(APPEND_TO_OFFLINE_ZONE, 1, offlineZoneKey,
                    String.valueOf(offlineOptions.getMaxBufferSize()).getBytes(), // @@ 1
                    bearMode, // @@ 2
                    data); // @@ 3
            if (rev == 0) {
                throw new MqttException(MqttException.REASON_CODE_OFFLINE_BUFFER_FULL);
            }
        }
    }

    @Override
    public MqttMessage popFromOfflineZone() {
        byte[] data;
        try (Jedis jedis = jedisPool.getResource()) {
            data = jedis.rpop(offlineZoneKey);
        }
        return data == null ? null : MqttDecoder.decode(data);
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
        } while (isPacketIdInUse(packetId));
        setPacketIdInUse(packetId);
        return packetId;
    }

    private boolean isPacketIdInUse(int packetId) {
        return inUsePacketIds.contains(packetId);
    }

    private void setPacketIdInUse(int packetId) {
        inUsePacketIds.add(packetId);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(inUsePacketIdsKey, String.valueOf(packetId));
        }
    }

    @Override
    public void releasePacketId(int packetId) {
        inUsePacketIds.remove(packetId);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.srem(inUsePacketIdsKey, String.valueOf(packetId));
        }
    }

    @Override
    public void inFlight(int packetId) throws MqttException {
        try (Jedis jedis = jedisPool.getResource()) {
            long rev = (long) jedis.eval(IN_FLIGHT, 1, inFlightZoneKey, String.valueOf(maxInFlight), String.valueOf(packetId));
            if (rev == 0) {
                throw new MqttException(MqttException.REASON_CODE_MAX_INFLIGHT);
            }
        }
    }

    @Override
    public boolean inFlightAck(int packetId) {
        inUsePacketIds.remove(packetId);
        try (Jedis jedis = jedisPool.getResource()) {
            long rev = (long) jedis.eval(IN_FLIGHT_ACK, 2, inUsePacketIdsKey, inFlightZoneKey, String.valueOf(packetId));
            return rev == 1;
        }
    }

    @Override
    public int getInFlightCount() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.scard(inFlightZoneKey).intValue();
        }
    }

    @Override
    public Set<Integer> getInFlightPacketIds() {
        return getPendingZonePacketIds(inFlightZoneKey);
    }

    @Override
    public void waitingForRec(int packetId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(waitingRecZoneKey, String.valueOf(packetId));
        }
    }

    @Override
    public void recReceived(int packetId) throws MqttException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.srem(waitingRecZoneKey, String.valueOf(packetId));
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
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.scard(waitingRecZoneKey).intValue();
        }
    }

    @Override
    public Set<Integer> getWaitingRecPacketIds() {
        return getPendingZonePacketIds(waitingRecZoneKey);
    }

    private Set<Integer> getPendingZonePacketIds(String zoneKey) {
        Set<String> set;
        try (Jedis jedis = jedisPool.getResource()) {
            set = jedis.smembers(zoneKey);
        }
        if (set == null || set.isEmpty()) {
            return new HashSet<>(0);
        }
        Set<Integer> packetIds = new HashSet<>();
        for (String s : set) {
            packetIds.add(Integer.valueOf(s));
        }
        return packetIds;
    }
}
