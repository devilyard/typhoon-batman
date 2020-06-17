/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubscribeMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#subscribe">MQTTV3.1/subscribe</a>
 *
 * @author C.
 */
public final class MqttSubscribeMessage extends MqttMessage {

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader, MqttSubscribePayload payload) {
        super(mqttFixedHeader, variableHeader, payload);
    }

    public MqttSubscribeMessage(MqttPacketIdVariableHeader variableHeader, MqttSubscribePayload payload) {
        super(new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0), variableHeader, payload);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }

    @Override
    public MqttSubscribePayload payload() {
        return (MqttSubscribePayload) super.payload();
    }
}