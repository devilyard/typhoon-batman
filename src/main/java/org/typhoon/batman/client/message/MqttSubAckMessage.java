/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubAckMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#suback">MQTTV3.1/suback</a>
 *
 * @author C.
 */
public final class MqttSubAckMessage extends MqttMessage {

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader, MqttSubAckPayload payload) {
        super(mqttFixedHeader, variableHeader, payload);
    }

    public MqttSubAckMessage(MqttPacketIdVariableHeader variableHeader, MqttSubAckPayload payload) {
        super(new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0), variableHeader, payload);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }

    @Override
    public MqttSubAckPayload payload() {
        return (MqttSubAckPayload) super.payload();
    }
}