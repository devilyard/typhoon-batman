/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubAckPayload.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#suback">MQTTV3.1/suback</a>
 *
 * @author C.
 */
final public class MqttSubAckPayload {

    private final List<Byte> grantedQoSLevels;

    public MqttSubAckPayload(int... grantedQoSLevels) {
        if (grantedQoSLevels == null) {
            throw new NullPointerException("grantedQoSLevels");
        }

        List<Byte> list = new ArrayList<>(grantedQoSLevels.length);
        for (int v: grantedQoSLevels) {
            list.add((byte) v);
        }
        this.grantedQoSLevels = Collections.unmodifiableList(list);
    }

    public MqttSubAckPayload(Iterable<Integer> grantedQoSLevels) {
        if (grantedQoSLevels == null) {
            throw new NullPointerException("grantedQoSLevels");
        }
        List<Byte> list = new ArrayList<>();
        for (Integer v: grantedQoSLevels) {
            if (v == null) {
                break;
            }
            list.add(v.byteValue());
        }
        this.grantedQoSLevels = Collections.unmodifiableList(list);
    }

    public List<Byte> grantedQoSLevels() {
        return grantedQoSLevels;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[grantedQoSLevels=" + grantedQoSLevels + ']';
    }
}