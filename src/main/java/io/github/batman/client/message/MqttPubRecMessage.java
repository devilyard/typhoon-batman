/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubRecMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#pubrec">MQTTV3.1/pubrec</a>
 *
 * @author C.
 */
public final class MqttPubRecMessage extends MqttMessage {

    public MqttPubRecMessage(MqttFixedHeader mqttFixedHeader, MqttPacketIdVariableHeader variableHeader) {
        super(mqttFixedHeader, variableHeader);
    }

    public MqttPubRecMessage(MqttPacketIdVariableHeader variableHeader) {
        super(new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2), variableHeader);
    }

    @Override
    public MqttPacketIdVariableHeader variableHeader() {
        return (MqttPacketIdVariableHeader) super.variableHeader();
    }
}
