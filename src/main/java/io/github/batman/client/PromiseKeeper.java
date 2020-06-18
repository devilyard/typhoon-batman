/*
 * Copyright (c) 2019. All rights reserved.
 * PromiseKeeper.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttMessageType;
import io.github.batman.client.message.MqttPacketIdVariableHeader;
import io.github.batman.client.message.MqttPublishVariableHeader;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author C.
 */
public class PromiseKeeper {

    private final Map<String, Promise<?>> promises = new ConcurrentHashMap<>();

    public void keep(MqttMessage message, Promise<?> promise) {
        promises.put(getPromiseKey(message), promise);
    }

    public Promise<?> retrieve(MqttMessage message) {
        return promises.get(getPromiseKey(message));
    }

    public boolean contains(MqttMessage message) {
        return promises.containsKey(getPromiseKey(message));
    }

    public Promise<?> remove(MqttMessage message) {
        return promises.remove(getPromiseKey(message));
    }

    public boolean isEmpty() {
        return promises.isEmpty();
    }

    public Collection<Promise<?>> getPromises() {
        return promises.values();
    }

    /**
     *
     * @param message
     * @return
     */
    private String getPromiseKey(MqttMessage message) {
        MqttMessageType messageType = message.fixedHeader().messageType();
        if (messageType == MqttMessageType.CONNECT || messageType == MqttMessageType.CONNACK) {
            return MqttMessageType.CONNECT.name();
        }
        if (messageType == MqttMessageType.DISCONNECT) {
            return messageType.name();
        }
        if (messageType == MqttMessageType.PUBLISH) {
            return String.valueOf(((MqttPublishVariableHeader) message.variableHeader()).packetId());
        }
        if (message.variableHeader() instanceof MqttPacketIdVariableHeader) {
            return String.valueOf(((MqttPacketIdVariableHeader)message.variableHeader()).packetId());
        }
        throw new UnsupportedOperationException(messageType.name());
    }
}
