/*
 * Copyright (c) 2019. All rights reserved.
 * MqttDecoder.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.*;
import org.typhoon.batman.client.message.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.github.batman.Constants.MAX_PACKET_ID;
import static io.github.batman.client.network.MqttCodecUtil.*;

/**
 * Decodes Mqtt messages from bytes, following
 * <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html">the MQTT protocol specification v3.1</a>
 *
 * @author C.
 */
public final class MqttDecoder {

    private static final int MAX_BYTES_IN_MESSAGE = 268435455;

    public static MqttMessage decode(byte[] buffer) {
        MqttDecodeInputStream inputStream = new MqttDecodeInputStream(buffer);
        MqttFixedHeader mqttFixedHeader = decodeFixedHeader(inputStream);

        if (mqttFixedHeader.remainingLength() > MAX_BYTES_IN_MESSAGE) {
            throw new MqttMessageDecodeException("too large message: " + mqttFixedHeader.remainingLength() + " bytes");
        }
        if (inputStream.remain() != mqttFixedHeader.remainingLength()) {
            throw new MqttMessageDecodeException("Invalid message payload, expected: " + mqttFixedHeader.remainingLength() + " bytes, but: " + inputStream.remain() + " bytes");
        }
        final MqttVariableHeader variableHeader = decodeVariableHeader(inputStream, mqttFixedHeader);
        final Object payload = decodePayload(inputStream, mqttFixedHeader.messageType(), variableHeader);
        return MqttMessageFactory.newMessage(mqttFixedHeader, variableHeader, payload);
    }

    /**
     * Decodes the fixed header. It's one byte for the flags and then variable bytes for the remaining length.
     *
     * @param inputStream the inputStream contains buffer to decode from
     * @return the fixed header
     */
    private static MqttFixedHeader decodeFixedHeader(MqttDecodeInputStream inputStream) {
        short b1 = inputStream.readUnsignedByte();

        MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
        boolean dupFlag = (b1 & 0x08) == 0x08;
        int qosLevel = (b1 & 0x06) >> 1;
        boolean retain = (b1 & 0x01) != 0;

        int remainingLength = 0;
        int multiplier = 1;
        short digit;
        int loops = 0;
        do {
            digit = inputStream.readUnsignedByte();
            remainingLength += (digit & 127) * multiplier;
            multiplier *= 128;
            loops++;
        } while ((digit & 128) != 0 && loops < 4);

        // MQTT protocol limits Remaining Length to 4 bytes
        if (loops == 4 && (digit & 128) != 0) {
            throw new MqttMessageDecodeException("remaining length exceeds 4 digits (" + messageType + ')');
        }
        MqttFixedHeader decodedFixedHeader = new MqttFixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
        return validateFixedHeader(resetUnusedFields(decodedFixedHeader));
    }

    /**
     * Decodes the variable header (if any)
     *
     * @param inputStream     the buffer to decode from
     * @param mqttFixedHeader MqttFixedHeader of the same message
     * @return the variable header
     */
    private static MqttVariableHeader decodeVariableHeader(MqttDecodeInputStream inputStream, MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.messageType()) {
            case CONNECT:
                return decodeConnectionVariableHeader(inputStream);

            case CONNACK:
                return decodeConnAckVariableHeader(inputStream);

            case SUBSCRIBE:
            case UNSUBSCRIBE:
            case SUBACK:
            case UNSUBACK:
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case PUBREL:
                return decodePacketIdVariableHeader(inputStream);

            case PUBLISH:
                return decodePublishVariableHeader(inputStream, mqttFixedHeader);

            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                return null;
        }
        return null; //should never reach here
    }

    private static MqttConnectVariableHeader decodeConnectionVariableHeader(MqttDecodeInputStream inputStream) {
        final String protocolName = decodeString(inputStream);
        final byte protocolLevel = inputStream.readByte();
        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(protocolName, protocolLevel);

        final int b1 = inputStream.readUnsignedByte();
        final int keepAlive = decodeMsbLsb(inputStream);

        final boolean hasUserName = (b1 & 0x80) == 0x80;
        final boolean hasPassword = (b1 & 0x40) == 0x40;
        final boolean willRetain = (b1 & 0x20) == 0x20;
        final int willQos = (b1 & 0x18) >> 3;
        final boolean willFlag = (b1 & 0x04) == 0x04;
        final boolean cleanSession = (b1 & 0x02) == 0x02;
        if (mqttVersion == MqttVersion.MQTT_3_1_1) {
            final boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
            if (!zeroReservedFlag) {
                // MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
                // set to zero and disconnect the Client if it is not zero.
                // See http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
                throw new MqttMessageDecodeException("non-zero reserved flag");
            }
        }

        return new MqttConnectVariableHeader(
                mqttVersion.protocolName(),
                mqttVersion.protocolLevel(),
                hasUserName,
                hasPassword,
                willRetain,
                willQos,
                willFlag,
                cleanSession,
                keepAlive);
    }

    private static MqttConnAckVariableHeader decodeConnAckVariableHeader(MqttDecodeInputStream inputStream) {
        final boolean sessionPresent = (inputStream.readUnsignedByte() & 0x01) == 0x01;
        byte returnCode = inputStream.readByte();
        return new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
    }

    private static MqttPacketIdVariableHeader decodePacketIdVariableHeader(MqttDecodeInputStream inputStream) {
        final int packetId = decodePacketId(inputStream);
        return new MqttPacketIdVariableHeader(packetId);
    }

    private static MqttPublishVariableHeader decodePublishVariableHeader(MqttDecodeInputStream inputStream, MqttFixedHeader mqttFixedHeader) {
        final String topic = decodeString(inputStream);
        if (!isValidPublishTopicName(topic)) {
            throw new MqttMessageDecodeException("invalid publish topic name: " + topic + " (contains wildcards)");
        }
        int packetId = -1;
        if (mqttFixedHeader.qosLevel().value() > 0) {
            packetId = decodePacketId(inputStream);
        }
        return new MqttPublishVariableHeader(topic, packetId);
    }

    private static int decodePacketId(MqttDecodeInputStream inputStream) {
        final int packetId = decodeMsbLsb(inputStream);
        if (!isValidPacketId(packetId)) {
            throw new MqttMessageDecodeException("invalid packetId: " + packetId);
        }
        return packetId;
    }

    /**
     * Decodes the payload.
     *
     * @param inputStream                  the buffer to decode from
     * @param messageType                  type of the message being decoded
     * @param variableHeader               variable header of the same message
     * @return the payload
     */
    private static Object decodePayload(MqttDecodeInputStream inputStream, MqttMessageType messageType, Object variableHeader) {
        switch (messageType) {
            case CONNECT:
                return decodeConnectionPayload(inputStream, (MqttConnectVariableHeader) variableHeader);

            case SUBSCRIBE:
                return decodeSubscribePayload(inputStream);

            case SUBACK:
                return decodeSubackPayload(inputStream);

            case UNSUBSCRIBE:
                return decodeUnsubscribePayload(inputStream);

            case PUBLISH:
                return decodePublishPayload(inputStream);

            default:
                // unknown payload , no byte consumed
                return null;
        }
    }

    private static MqttConnectPayload decodeConnectionPayload(MqttDecodeInputStream inputStream, MqttConnectVariableHeader mqttConnectVariableHeader) {
        final String clientId = decodeString(inputStream);
        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(mqttConnectVariableHeader.name(),
                (byte) mqttConnectVariableHeader.version());
        if (!isValidClientId(mqttVersion, clientId)) {
            throw new MqttInvalidClientIdentifierException("invalid clientIdentifier: " + clientId);
        }
        String willTopic = null;
        byte[] willMessage = null;
        if (mqttConnectVariableHeader.isWillFlag()) {
            willTopic = decodeString(inputStream, 0, 32767);
            willMessage = decodeByteArray(inputStream);
        }
        String userName = null;
        byte[] password = null;
        if (mqttConnectVariableHeader.hasUserName()) {
            userName = decodeString(inputStream);
        }
        if (mqttConnectVariableHeader.hasPassword()) {
            password = decodeByteArray(inputStream);
        }

        return new MqttConnectPayload(clientId, willTopic, willMessage, userName, password);
    }

    private static MqttSubscribePayload decodeSubscribePayload(MqttDecodeInputStream inputStream) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
        while (inputStream.remain() > 0) {
            final String topicName = decodeString(inputStream);
            int qos = inputStream.readUnsignedByte() & 0x03;
            subscribeTopics.add(new MqttTopicSubscription(topicName, MqttQoS.valueOf(qos)));
        }
        return new MqttSubscribePayload(subscribeTopics);
    }

    private static MqttSubAckPayload decodeSubackPayload(MqttDecodeInputStream inputStream) {
        final List<Integer> grantedQos = new ArrayList<>();
        while (inputStream.remain() > 0) {
            int qos = inputStream.readUnsignedByte() & 0x03;
            grantedQos.add(qos);
        }
        return new MqttSubAckPayload(grantedQos);
    }

    private static MqttUnsubscribePayload decodeUnsubscribePayload(MqttDecodeInputStream inputStream) {
        final List<String> unsubscribeTopics = new ArrayList<>();
        while (inputStream.remain() > 0) {
            String topicName = decodeString(inputStream);
            unsubscribeTopics.add(topicName);
        }
        return new MqttUnsubscribePayload(unsubscribeTopics);
    }

    private static byte[] decodePublishPayload(MqttDecodeInputStream inputStream) {
        return inputStream.readBytes(inputStream.remain());
    }

    private static String decodeString(MqttDecodeInputStream inputStream) {
        return decodeString(inputStream, 0, Integer.MAX_VALUE);
    }

    private static String decodeString(MqttDecodeInputStream inputStream, int minBytes, int maxBytes) {
        final int size = decodeMsbLsb(inputStream);
        if (size < minBytes || size > maxBytes) {
            inputStream.skipBytes(size);
            return null;
        }
        return inputStream.readString(size, StandardCharsets.UTF_8);
    }

    private static byte[] decodeByteArray(MqttDecodeInputStream inputStream) {
        final int size = decodeMsbLsb(inputStream);
        return inputStream.readBytes(size);
    }

    private static int decodeMsbLsb(MqttDecodeInputStream inputStream) {
        return decodeMsbLsb(inputStream, 0, MAX_PACKET_ID);
    }

    private static int decodeMsbLsb(MqttDecodeInputStream inputStream, int min, int max) {
        short msbSize = inputStream.readUnsignedByte();
        short lsbSize = inputStream.readUnsignedByte();
        int result = msbSize << 8 | lsbSize;
        if (result < min || result > max) {
            result = -1;
        }
        return result;
    }

    private static final class MqttDecodeInputStream {

        private final byte[] buffer;
        private int readIndex;

        MqttDecodeInputStream(byte[] buffer) {
            this.buffer = buffer;
        }

        public byte readByte() {
            return buffer[readIndex++];
        }

        public short readUnsignedByte() {
            return (short) (buffer[readIndex++] & 0xff);
        }

        public byte[] readBytes(int length) {
            if (length <= 0) {
                return null;
            }
            length = Math.min(length, buffer.length - readIndex);
            byte[] bytes = new byte[length];
            System.arraycopy(buffer, readIndex, bytes, 0, length);
            readIndex += length;
            return bytes;
        }

        public void skipBytes(int length) {
            readIndex += length;
        }

        public String readString(int length, Charset charset) {
            length = Math.min(length, buffer.length - readIndex);
            String s = new String(buffer, readIndex, length, charset);
            readIndex += length;
            return s;
        }

        public int remain() {
            return buffer.length - readIndex;
        }
    }
}
