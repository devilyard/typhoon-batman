/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPingReqMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#pingreq">MQTTV3.1/pingreq</a>
 *
 * @author C.
 */
public final class MqttPingReqMessage extends MqttMessage {

    public MqttPingReqMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPingReqMessage() {
        super(new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}
