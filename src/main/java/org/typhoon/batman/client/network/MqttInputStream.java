/*
 * Copyright (c) 2019. All rights reserved.
 * MqttInputStream.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.message.MqttMessage;
import org.typhoon.batman.client.message.MqttMessageType;

import java.io.*;
import java.net.SocketTimeoutException;

/**
 * @author C.
 */
public class MqttInputStream extends InputStream {

    private final DataInputStream inputStream;
    private int remainLen;
    private int packetLen;
    private byte[] packet;
    private int fixedHeaderLength;

    public MqttInputStream(InputStream inputStream) {
        if (inputStream instanceof DataInputStream) {
            this.inputStream = (DataInputStream) inputStream;
        } else {
            this.inputStream = new DataInputStream(inputStream);
        }
        this.remainLen = -1;
    }

    public int read() throws IOException {
        return inputStream.read();
    }

    public int available() throws IOException {
        return inputStream.available();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public MqttMessage readMqtt() throws IOException, MqttException {
        try {
            if (remainLen < 0) {
                byte first = inputStream.readByte();
                byte type = (byte) ((first >>> 4) & 0x0F);
                if ((type < MqttMessageType.CONNECT.value()) || (type > MqttMessageType.DISCONNECT.value())) {
                    throw new MqttException(MqttException.REASON_CODE_INVALID_MESSAGE);
                }
                remainLen = readRemainLength();
                byte[] remainLengthBytes = encodeRemainLength();
                fixedHeaderLength = 1 + remainLengthBytes.length;
                packet = new byte[fixedHeaderLength + remainLen];
                packet[0] = first;
                System.arraycopy(remainLengthBytes, 0, packet, 1, remainLengthBytes.length);
                packetLen = 0;
            } else {
                readFully();
                remainLen = -1;
                return MqttDecoder.decode(packet);
            }
        } catch (SocketTimeoutException ignore) {
        }
        return null;
    }

    private int readRemainLength() throws IOException, MqttException {
        byte digit;
        int msgLength = 0;
        int multiplier = 1;
        int loops = 0;
        do {
            digit = inputStream.readByte();
            msgLength += ((digit & 0x7F) * multiplier);
            multiplier *= 128;
            loops++;
        } while ((digit & 0x80) != 0 && loops < 4);
        if (loops == 4 && (digit & 128) != 0) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_MESSAGE);
        }
        return msgLength;
    }

    private byte[] encodeRemainLength() {
        int len = remainLen;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        do {
            byte digit = (byte) (len % 128);
            len = len / 128;
            if (len > 0) {
                digit |= 0x80;
            }
            bos.write(digit);
        } while (len > 0);

        return bos.toByteArray();
    }

    private void readFully() throws IOException {
        int off = fixedHeaderLength + packetLen;
        int len = remainLen - packetLen;
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count;
            try {
                count = inputStream.read(packet, off + n, len - n);
            } catch (SocketTimeoutException e) {
                packetLen += n;
                throw e;
            }

            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }
}
