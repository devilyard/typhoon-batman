/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPacketIdVariableHeader.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

/**
 * Variable Header containing only Message Id
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#msg-id">MQTTV3.1/msg-id</a>
 * See <a href="http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718025">MQTTV3.1.1/Packet Identifier</a>
 *
 * @author C.
 */
public final class MqttPacketIdVariableHeader implements MqttVariableHeader {

    private final int packetId;

    public MqttPacketIdVariableHeader(int packetId) {
        if (packetId < 1 || packetId > 0xffff) {
            throw new IllegalArgumentException("packetId: " + packetId + " (expected: 1 ~ 65535)");
        }
        this.packetId = packetId;
    }

    public int packetId() {
        return packetId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packetId=" + packetId + ']';
    }
}