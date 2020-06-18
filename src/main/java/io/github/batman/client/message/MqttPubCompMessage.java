/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubCompMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#pubcomp">MQTTV3.1/pubcomp</a>
 *
 * @author C.
 */
public final class MqttPubCompMessage extends MqttMessage {

    public MqttPubCompMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader) {
        super(mqttFixedHeader, variableHeader);
    }

    public MqttPubCompMessage(MqttPacketIdVariableHeader variableHeader) {
        super(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2), variableHeader);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }
}
