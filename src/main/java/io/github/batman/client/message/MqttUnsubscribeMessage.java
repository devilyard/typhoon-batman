/*
 * Copyright (c) 2019. All rights reserved.
 * MqttUnsubscribeMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsubscribe">MQTTV3.1/unsubscribe</a>
 *
 * @author C.
 */
public final class MqttUnsubscribeMessage extends MqttMessage {

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader, MqttUnsubscribePayload payload) {
        super(mqttFixedHeader, variableHeader, payload);
    }

    public MqttUnsubscribeMessage(MqttPacketIdVariableHeader variableHeader, MqttUnsubscribePayload payload) {
        super(new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0), variableHeader, payload);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }

    @Override
    public MqttUnsubscribePayload payload() {
        return (MqttUnsubscribePayload) super.payload();
    }
}