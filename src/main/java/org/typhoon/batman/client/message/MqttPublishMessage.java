/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPublishMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;


/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#publish">MQTTV3.1/publish</a>
 *
 * @author C.
 */
final public class MqttPublishMessage extends MqttMessage {

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader variableHeader, byte[] payload) {
        super(mqttFixedHeader, variableHeader, payload);
    }

    @Override
    public MqttPublishVariableHeader variableHeader() {
        return (MqttPublishVariableHeader) super.variableHeader();
    }

    @Override
    public byte[] payload() {
        return (byte[]) super.payload();
    }

}