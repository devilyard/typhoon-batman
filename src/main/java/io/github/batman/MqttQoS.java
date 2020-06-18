/*
 * Copyright (c) 2019. All rights reserved.
 * MqttQoS.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman;

/**
 * @author C.
 */
public enum MqttQoS {

    AT_MOST_ONCE((byte) 0x00),
    AT_LEAST_ONCE((byte) 0x01),
    EXACTLY_ONCE((byte) 0x02);

    private final byte value;

    MqttQoS(byte value) {
        this.value = value;
    }

    public byte value() {
        return value;
    }

    public static MqttQoS valueOf(int value) {
        if (value >= 0 && value <= 2) {
            return values()[value];
        }
        throw new IllegalArgumentException("invalid QoS: " + value);
    }
}
