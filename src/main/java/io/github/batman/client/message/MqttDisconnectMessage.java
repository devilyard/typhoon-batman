/*
 * Copyright (c) 2019. All rights reserved.
 * MqttDisconnectMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

import io.github.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#disconnect">MQTTV3.1/disconnect</a>
 *
 * @author C.
 */
public class MqttDisconnectMessage extends MqttMessage {

    public MqttDisconnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttDisconnectMessage() {
        super(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}
