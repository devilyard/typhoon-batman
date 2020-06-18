/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubAckMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#puback">MQTTV3.1/puback</a>
 *
 * @author C.
 */
public final class MqttPubAckMessage extends MqttMessage {

    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader) {
        super(mqttFixedHeader, variableHeader);
    }

    public MqttPubAckMessage(MqttPacketIdVariableHeader variableHeader) {
        super(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2), variableHeader);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }
}
