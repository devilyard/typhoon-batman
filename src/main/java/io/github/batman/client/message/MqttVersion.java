/*
 * Copyright (c) 2019. All rights reserved.
 * MqttVersion.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author C.
 */
public enum MqttVersion {

    MQTT_3_1("MQIsdp", (byte)0x03),
    MQTT_3_1_1("MQTT", (byte) 0x04);

    private final String name;
    private final byte level;

    MqttVersion(String protocolName, byte protocolLevel) {
        name = protocolName;
        level = protocolLevel;
    }

    public String protocolName() {
        return name;
    }

    public byte[] protocolNameBytes() {
        return name.getBytes(StandardCharsets.UTF_8);
    }

    public byte protocolLevel() {
        return level;
    }

    public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel) {
        for (MqttVersion mv : values()) {
            if (mv.name.equals(protocolName)) {
                if (mv.level == protocolLevel) {
                    return mv;
                } else {
                    throw new IllegalArgumentException(protocolName + " and " + protocolLevel + " are not match");
                }
            }
        }
        throw new IllegalArgumentException(protocolName + " is unknown protocol name");
    }
}