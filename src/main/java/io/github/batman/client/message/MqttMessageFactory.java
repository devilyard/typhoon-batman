/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageFactory.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.message;

/**
 * @author C.
 */
public class MqttMessageFactory {

    /**
     *
     * @param mqttFixedHeader
     * @param variableHeader
     * @param payload
     * @return
     */
    public static MqttMessage newMessage(MqttFixedHeader mqttFixedHeader, MqttVariableHeader variableHeader, Object payload) {
        switch (mqttFixedHeader.messageType()) {
            case CONNECT :
                return new MqttConnectMessage(mqttFixedHeader, (MqttConnectVariableHeader) variableHeader, (MqttConnectPayload) payload);

            case CONNACK:
                return new MqttConnAckMessage(mqttFixedHeader, (MqttConnAckVariableHeader) variableHeader);

            case SUBSCRIBE:
                return new MqttSubscribeMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader, (MqttSubscribePayload) payload);

            case SUBACK:
                return new MqttSubAckMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader, (MqttSubAckPayload) payload);

            case UNSUBACK:
                return new MqttUnsubAckMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader);

            case UNSUBSCRIBE:
                return new MqttUnsubscribeMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader, (MqttUnsubscribePayload) payload);

            case PUBLISH:
                return new MqttPublishMessage(mqttFixedHeader, (MqttPublishVariableHeader) variableHeader, (byte[]) payload);

            case PUBACK:
                return new MqttPubAckMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader);

            case PUBREC:
                return new MqttPubRecMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader);

            case PUBREL:
                return new MqttPubRelMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader);

            case PUBCOMP:
                return new MqttPubCompMessage(mqttFixedHeader, (MqttPacketIdVariableHeader) variableHeader);

            case PINGREQ:
                return new MqttPingReqMessage(mqttFixedHeader);

            case PINGRESP:
                return new MqttPingRespMessage(mqttFixedHeader);

            case DISCONNECT:
                return new MqttDisconnectMessage(mqttFixedHeader);

            default:
                throw new IllegalArgumentException("unknown message type: " + mqttFixedHeader.messageType());
        }
    }
}
