/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageType.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#fixed-header">MQTTV3.1/fixed-header</a>
 *
 * @author C.
 */
public enum MqttMessageType {

    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14);

    private final int value;

    MqttMessageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MqttMessageType valueOf(int type) {
        if (type < CONNECT.value || type > DISCONNECT.value) {
            throw new IllegalArgumentException("unknown message type: " + type);
        }
        return values()[type - 1];
    }
}