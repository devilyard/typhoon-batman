/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubRelMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#pubrel">MQTTV3.1/pubrel</a>
 *
 * @author C.
 */
final public class MqttPubRelMessage extends MqttMessage {

    public MqttPubRelMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader) {
        super(mqttFixedHeader, variableHeader);
    }

    public MqttPubRelMessage(MqttPacketIdVariableHeader variableHeader) {
        super(new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2), variableHeader);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }
}
