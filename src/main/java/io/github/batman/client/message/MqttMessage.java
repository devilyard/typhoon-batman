/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessage.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#msg-format">MQTTV3.1/message format</a>
 *
 * @author C.
 */
abstract public class MqttMessage {

    private final MqttFixedHeader mqttFixedHeader;
    private final MqttVariableHeader variableHeader;
    private final Object payload;

    public MqttMessage(MqttFixedHeader mqttFixedHeader) {
        this(mqttFixedHeader, null, null);
    }

    public MqttMessage(MqttFixedHeader mqttFixedHeader, MqttVariableHeader variableHeader) {
        this(mqttFixedHeader, variableHeader, null);
    }

    public MqttMessage(MqttFixedHeader mqttFixedHeader, MqttVariableHeader variableHeader, Object payload) {
        this.mqttFixedHeader = mqttFixedHeader;
        this.variableHeader = variableHeader;
        this.payload = payload;
    }

    public MqttFixedHeader fixedHeader() {
        return mqttFixedHeader;
    }

    public MqttVariableHeader variableHeader() {
        return variableHeader;
    }

    public Object payload() {
        return payload;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[fixedHeader=" + (fixedHeader() != null ? fixedHeader().toString() : "") +
                ", variableHeader=" + (variableHeader() != null ? variableHeader.toString() : "") +
                ", payload=" + (payload() != null ? payload.toString() : "") +
                ']';
    }
}
