/*
 * Copyright (c) 2019. All rights reserved.
 * MqttCodecUtil.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import org.typhoon.batman.Constants;
import org.typhoon.batman.MqttQoS;
import org.typhoon.batman.client.message.MqttFixedHeader;
import org.typhoon.batman.client.message.MqttVersion;

import static org.typhoon.batman.Constants.MAX_PACKET_ID;

/**
 * @author C.
 */
final class MqttCodecUtil {

    static boolean isValidPublishTopicName(String topicName) {
        for (char c : Constants.TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidPacketId(int packetId) {
        return packetId > 0 && packetId <= MAX_PACKET_ID;
    }

    static boolean isValidClientId(MqttVersion mqttVersion, String clientId) {
        if (mqttVersion == MqttVersion.MQTT_3_1) {
            return clientId != null && clientId.length() >= Constants.MIN_CLIENT_ID_LENGTH_V3_1 && clientId.length() <= Constants.MAX_CLIENT_ID_LENGTH_V3_1;
        }
        if (mqttVersion == MqttVersion.MQTT_3_1_1) {
            // In 3.1.1 Client Identifier of MQTT 3.1.1 specification, The Server MAY allow ClientId’s
            // that contain more than 23 encoded bytes. And, The Server MAY allow zero-length ClientId.
            return clientId != null && clientId.length() <= Constants.MAX_CLIENT_ID_LENGTH_V3_1_1;
        }
        throw new IllegalArgumentException(mqttVersion + " is unknown mqtt version");
    }

    static MqttFixedHeader validateFixedHeader(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.messageType()) {
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (mqttFixedHeader.qosLevel() != MqttQoS.AT_LEAST_ONCE) {
                    throw new MqttMessageDecodeException(mqttFixedHeader.messageType().name() + " message must have QoS " + MqttQoS.AT_LEAST_ONCE.value());
                }
            default:
                return mqttFixedHeader;
        }
    }

    static MqttFixedHeader resetUnusedFields(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.messageType()) {
            case CONNECT:
            case CONNACK:
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case SUBACK:
            case UNSUBACK:
            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                if (mqttFixedHeader.isDup() || mqttFixedHeader.qosLevel() != MqttQoS.AT_MOST_ONCE || mqttFixedHeader.isRetain()) {
                    return new MqttFixedHeader(mqttFixedHeader.messageType(), false, MqttQoS.AT_MOST_ONCE, false, mqttFixedHeader.remainingLength());
                }
                return mqttFixedHeader;
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (mqttFixedHeader.isDup() || mqttFixedHeader.isRetain() || mqttFixedHeader.qosLevel() != MqttQoS.AT_LEAST_ONCE) {
                    return new MqttFixedHeader(mqttFixedHeader.messageType(), false, MqttQoS.AT_LEAST_ONCE, false, mqttFixedHeader.remainingLength());
                }
                return mqttFixedHeader;
            default:
                return mqttFixedHeader;
        }
    }

    private MqttCodecUtil() { }
}
