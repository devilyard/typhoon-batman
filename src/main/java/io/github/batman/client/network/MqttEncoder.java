/*
 * Copyright (c) 2019. All rights reserved.
 * MqttEncoder.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.*;

import java.nio.charset.StandardCharsets;

/**
 * Encodes Mqtt messages into bytes following the protocol specification v3.1
 * as described here <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html">MQTTV3.1</a>
 *
 * @author C.
 */
public final class MqttEncoder {

    private static final byte[] EMPTY_BYTES = new byte[0];

    private MqttEncoder() {
    }

    public static byte[] encode(MqttMessage msg) {
        return doEncode(msg);
    }

    private static byte[] doEncode(MqttMessage message) {
        switch (message.fixedHeader().messageType()) {
            case CONNECT:
                return encodeConnectMessage((MqttConnectMessage) message);

            case CONNACK:
                return encodeConnAckMessage((MqttConnAckMessage) message);

            case PUBLISH:
                return encodePublishMessage((MqttPublishMessage) message);

            case SUBSCRIBE:
                return encodeSubscribeMessage((MqttSubscribeMessage) message);

            case UNSUBSCRIBE:
                return encodeUnsubscribeMessage((MqttUnsubscribeMessage) message);

            case SUBACK:
                return encodeSubAckMessage((MqttSubAckMessage) message);

            case UNSUBACK:
            case PUBACK:
            case PUBREC:
            case PUBREL:
            case PUBCOMP:
                return encodeMessageWithSingleByteFixedHeaderAndPacketId(message);

            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                return encodeMessageWithSingleByteFixedHeader(message);

            default:
                throw new IllegalArgumentException("Unknown message type: " + message.fixedHeader().messageType().value());
        }
    }

    private static byte[] encodeConnectMessage(MqttConnectMessage message) {
        int payloadBufferSize = 0;
        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttConnectVariableHeader variableHeader = message.variableHeader();
        MqttConnectPayload payload = message.payload();
        MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(variableHeader.name(), (byte) variableHeader.version());
        String clientIdentifier = payload.clientIdentifier();
        if (!MqttCodecUtil.isValidClientId(mqttVersion, clientIdentifier)) {
            throw new MqttInvalidClientIdentifierException("invalid clientIdentifier: " + clientIdentifier);
        }
        byte[] clientIdentifierBytes = encodeStringUtf8(clientIdentifier);
        payloadBufferSize += 2 + clientIdentifierBytes.length;

        // Will topic and message
        String willTopic = payload.willTopic();
        byte[] willTopicBytes = willTopic != null ? encodeStringUtf8(willTopic) : EMPTY_BYTES;
        byte[] willMessage = payload.willMessageInBytes();
        byte[] willMessageBytes = willMessage != null ? willMessage : EMPTY_BYTES;
        if (variableHeader.isWillFlag()) {
            payloadBufferSize += 2 + willTopicBytes.length;
            payloadBufferSize += 2 + willMessageBytes.length;
        }

        String userName = payload.userName();
        byte[] userNameBytes = userName != null ? encodeStringUtf8(userName) : EMPTY_BYTES;
        if (variableHeader.hasUserName()) {
            payloadBufferSize += 2 + userNameBytes.length;
        }

        byte[] password = payload.passwordInBytes();
        byte[] passwordBytes = password != null ? password : EMPTY_BYTES;
        if (variableHeader.hasPassword()) {
            payloadBufferSize += 2 + passwordBytes.length;
        }

        // Fixed header
        byte[] protocolNameBytes = mqttVersion.protocolNameBytes();
        int variableHeaderBufferSize = 2 + protocolNameBytes.length + 4;
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variablePartSize);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(outputStream, variablePartSize);
        outputStream.writeShort((short) protocolNameBytes.length);
        outputStream.writeBytes(protocolNameBytes);

        outputStream.writeByte((byte) variableHeader.version());
        outputStream.writeByte((byte) getConnVariableHeaderFlag(variableHeader));
        outputStream.writeShort((short) variableHeader.keepAliveTimeSeconds());

        // Payload
        outputStream.writeShort((short) clientIdentifierBytes.length);
        outputStream.writeBytes(clientIdentifierBytes);
        if (variableHeader.isWillFlag()) {
            outputStream.writeShort((short) willTopicBytes.length);
            outputStream.writeBytes(willTopicBytes);
            outputStream.writeShort((short) willMessageBytes.length);
            outputStream.writeBytes(willMessageBytes);
        }
        if (variableHeader.hasUserName()) {
            outputStream.writeShort((short) userNameBytes.length);
            outputStream.writeBytes(userNameBytes);
        }
        if (variableHeader.hasPassword()) {
            outputStream.writeShort((short) passwordBytes.length);
            outputStream.writeBytes(passwordBytes);
        }
        return outputStream.toByteArray();
    }

    private static int getConnVariableHeaderFlag(MqttConnectVariableHeader variableHeader) {
        int flagByte = 0;
        if (variableHeader.hasUserName()) {
            flagByte |= 0x80;
        }
        if (variableHeader.hasPassword()) {
            flagByte |= 0x40;
        }
        if (variableHeader.isWillRetain()) {
            flagByte |= 0x20;
        }
        flagByte |= (variableHeader.willQos() & 0x03) << 3;
        if (variableHeader.isWillFlag()) {
            flagByte |= 0x04;
        }
        if (variableHeader.isCleanSession()) {
            flagByte |= 0x02;
        }
        return flagByte;
    }

    private static byte[] encodeConnAckMessage(MqttConnAckMessage message) {
        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(4);
        outputStream.writeByte(getFixedHeaderByte1(message.fixedHeader()));
        outputStream.writeByte((byte) 2);
        outputStream.writeByte((byte) (message.variableHeader().isSessionPresent() ? 0x01 : 0x00));
        outputStream.writeByte(message.variableHeader().connectReturnCode().value());

        return outputStream.toByteArray();
    }

    private static byte[] encodeSubscribeMessage(MqttSubscribeMessage message) {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = 0;

        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttPacketIdVariableHeader variableHeader = message.variableHeader();
        MqttSubscribePayload payload = message.payload();

        for (MqttTopicSubscription topic : payload.topicSubscriptions()) {
            String topicName = topic.topicName();
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            payloadBufferSize += 2 + topicNameBytes.length;
            payloadBufferSize += 1; // @@ qos byte
        }

        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variablePartSize);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(outputStream, variablePartSize);

        // Variable Header
        int packetId = variableHeader.packetId();
        outputStream.writeShort((short) packetId);

        // Payload
        for (MqttTopicSubscription topic : payload.topicSubscriptions()) {
            String topicName = topic.topicName();
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            outputStream.writeShort((short) topicNameBytes.length);
            outputStream.writeBytes(topicNameBytes, 0, topicNameBytes.length);
            outputStream.writeByte(topic.qualityOfService().value());
        }

        return outputStream.toByteArray();
    }

    private static byte[] encodeUnsubscribeMessage(MqttUnsubscribeMessage message) {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = 0;

        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttPacketIdVariableHeader variableHeader = message.variableHeader();
        MqttUnsubscribePayload payload = message.payload();

        for (String topicName : payload.topics()) {
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            payloadBufferSize += 2 + topicNameBytes.length;
        }

        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variablePartSize);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(outputStream, variablePartSize);

        // Variable Header
        int packetId = variableHeader.packetId();
        outputStream.writeShort((short) packetId);

        // Payload
        for (String topicName : payload.topics()) {
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            outputStream.writeShort((short) topicNameBytes.length);
            outputStream.writeBytes(topicNameBytes, 0, topicNameBytes.length);
        }

        return outputStream.toByteArray();
    }

    private static byte[] encodeSubAckMessage(MqttSubAckMessage message) {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = message.payload().grantedQoSLevels().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variablePartSize);
        outputStream.writeByte(getFixedHeaderByte1(message.fixedHeader()));
        writeVariableLengthInt(outputStream, variablePartSize);
        outputStream.writeShort((short) message.variableHeader().packetId());
        for (byte qos : message.payload().grantedQoSLevels()) {
            outputStream.writeByte(qos);
        }

        return outputStream.toByteArray();
    }

    private static byte[] encodePublishMessage(MqttPublishMessage message) {
        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttPublishVariableHeader variableHeader = message.variableHeader();
        byte[] payload = message.payload();

        String topicName = variableHeader.topicName();
        byte[] topicNameBytes = encodeStringUtf8(topicName);

        int variableHeaderBufferSize = 2 + topicNameBytes.length + (mqttFixedHeader.qosLevel().value() > 0 ? 2 : 0);
        int payloadBufferSize = payload == null ? 0 : payload.length;
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variablePartSize);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(outputStream, variablePartSize);
        outputStream.writeShort((short) topicNameBytes.length);
        outputStream.writeBytes(topicNameBytes);
        if (mqttFixedHeader.qosLevel().value() > MqttQoS.AT_MOST_ONCE.value()) {
            outputStream.writeShort((short) variableHeader.packetId());
        }
        outputStream.writeBytes(payload);

        return outputStream.toByteArray();
    }

    private static byte[] encodeMessageWithSingleByteFixedHeaderAndPacketId(MqttMessage message) {
        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttPacketIdVariableHeader variableHeader = (MqttPacketIdVariableHeader) message.variableHeader();
        int msgId = variableHeader.packetId();

        int variableHeaderBufferSize = 2; // variable part only has a message id
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(fixedHeaderBufferSize + variableHeaderBufferSize);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(outputStream, variableHeaderBufferSize);
        outputStream.writeShort((short) msgId);

        return outputStream.toByteArray();
    }

    private static byte[] encodeMessageWithSingleByteFixedHeader(MqttMessage message) {
        MqttFixedHeader mqttFixedHeader = message.fixedHeader();
        MqttEncodeOutputStream outputStream = new MqttEncodeOutputStream(2);
        outputStream.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        outputStream.writeByte((byte) 0);

        return outputStream.toByteArray();
    }

    private static byte getFixedHeaderByte1(MqttFixedHeader header) {
        int ret = 0;
        ret |= header.messageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.qosLevel().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return (byte) ret;
    }

    private static void writeVariableLengthInt(MqttEncodeOutputStream outputStream, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            outputStream.writeByte((byte) digit);
        } while (num > 0);
    }

    private static int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num /= 128;
            count++;
        } while (num > 0);
        return count;
    }

    private static byte[] encodeStringUtf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private final static class MqttEncodeOutputStream {

        private final byte[] buffer;
        private int writeIndex;

        MqttEncodeOutputStream(int length) {
            buffer = new byte[length];
        }

        void writeByte(byte value) {
            buffer[writeIndex++] = value;
        }

        void writeShort(short value) {
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) ((byte) value & 0xff);
        }

        void writeBytes(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return;
            }
            System.arraycopy(bytes, 0, buffer, writeIndex, bytes.length);
            writeIndex += bytes.length;
        }

        void writeBytes(byte[] bytes, int offset, int length) {
            if (bytes == null || bytes.length == 0) {
                return;
            }
            if (offset >= bytes.length) {
                return;
            }
            System.arraycopy(bytes, offset, buffer, writeIndex, length);
            writeIndex += length;
        }

        public byte[] toByteArray() {
            return buffer;
        }
    }
}