/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnectReturnCode.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connack">MQTTV3.1/conack</a>
 *
 * @author C.
 */
public enum MqttConnectReturnCode {

    CONNECTION_ACCEPTED((byte) 0x00),
    CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05);

    private final byte value;

    MqttConnectReturnCode(byte value) {
        this.value = value;
    }

    public byte value() {
        return value;
    }

    public static MqttConnectReturnCode valueOf(byte b) {
        if (b < CONNECTION_ACCEPTED.value || b > CONNECTION_REFUSED_NOT_AUTHORIZED.value) {
            throw new IllegalArgumentException("unknown connect return code: " + (b & 0xFF));
        }
        return values()[b];
    }
}