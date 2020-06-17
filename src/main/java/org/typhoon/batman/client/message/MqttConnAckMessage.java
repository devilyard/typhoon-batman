/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnAckMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connack">MQTTV3.1/connack</a>
 *
 * @author C.
 */
public final class MqttConnAckMessage extends MqttMessage {

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader, MqttConnAckVariableHeader variableHeader) {
        super(mqttFixedHeader, variableHeader);
    }

    public MqttConnAckMessage(MqttConnAckVariableHeader variableHeader) {
        super(new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2), variableHeader);
    }

    @Override
    public MqttConnAckVariableHeader variableHeader() {
        return (MqttConnAckVariableHeader) super.variableHeader();
    }
}