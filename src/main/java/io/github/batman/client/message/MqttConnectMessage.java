/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnectMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connect">MQTTV3.1/connect</a>
 *
 * @author C.
 */
public final class MqttConnectMessage extends MqttMessage {

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader, MqttConnectVariableHeader variableHeader, MqttConnectPayload payload) {
        super(mqttFixedHeader, variableHeader, payload);
    }

    public MqttConnectMessage(MqttConnectVariableHeader variableHeader, MqttConnectPayload payload) {
        super(new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0), variableHeader, payload);
    }

    @Override
    public MqttConnectVariableHeader variableHeader() {
        return (MqttConnectVariableHeader) super.variableHeader();
    }

    @Override
    public MqttConnectPayload payload() {
        return (MqttConnectPayload) super.payload();
    }
}