/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPublishVariableHeader.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#publish">MQTTV3.1/publish</a>
 *
 * @author C.
 */
public final class MqttPublishVariableHeader implements MqttVariableHeader {

    private final String topicName;
    private final int packetId;

    public MqttPublishVariableHeader(String topicName, int packetId) {
        this.topicName = topicName;
        this.packetId = packetId;
    }

    public String topicName() {
        return topicName;
    }

    public int packetId() {
        return packetId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[topicName=" + topicName + ", packetId=" + packetId + ']';
    }
}