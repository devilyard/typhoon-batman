/*
 * Copyright (c) 2019. All rights reserved.
 * WebSocketFrame.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * See <a href="https://tools.ietf.org/html/rfc6455#section-5.2">WebSocket/Base Framing Protocol</a>
 *
 * @author C.
 */
public class WebSocketFrame {

    private static final int frameLengthOverhead = 6;

    private byte opcode;
    private boolean fin;
    private byte[] payload;
    private boolean closeFlag = false;

    /**
     * Initialise a new WebSocketFrame
     *
     * @param opcode  WebSocket Opcode
     * @param fin     If it is final
     * @param payload The payload
     */
    public WebSocketFrame(byte opcode, boolean fin, byte[] payload) {
        this.opcode = opcode;
        this.fin = fin;
        this.payload = payload;
    }

    /**
     * Initialise WebSocketFrame from raw Data
     *
     * @param rawFrame The raw byte buffer
     */
    public WebSocketFrame(byte[] rawFrame) throws IOException {
        // First Byte: Fin, Reserved, Opcode
        byte b = rawFrame[0];
        setFinAndOpCode(b);
        if (this.opcode == 2) {
            // Second Byte Masked & Initial Length
            b = rawFrame[1];
            boolean masked = ((b & 0x80) != 0);
            int payloadLength = (byte) (0x7F & b);
            int byteCount = 0;
            if (payloadLength == 0X7F) {
                // 8 Byte Extended payload length
                byteCount = 8;
            } else if (payloadLength == 0X7E) {
                // 2 bytes extended payload length
                byteCount = 2;
            }

            // Decode the extended payload length
            while (--byteCount > 0) {
                b = rawFrame[2];
                payloadLength |= (b & 0xFF) << (8 * byteCount);
            }

            // Get the Masking key if masked
            byte[] maskingKey = null;
            if (masked) {
                maskingKey = new byte[4];
                System.arraycopy(rawFrame, 3, maskingKey, 0, 4);
            }
            if (payloadLength > 0) {
                this.payload = new byte[payloadLength];
                System.arraycopy(rawFrame, 7, payload, 0, payloadLength);

                // De-mask payload if needed
                if (masked) {
                    for (int i = 0; i < this.payload.length; i++) {
                        this.payload[i] ^= maskingKey[i % 4];
                    }
                }
            }
        } else if (opcode == 8) {
            closeFlag = true;
        } else {
            throw new IOException("Invalid Frame: Opcode: " + this.opcode);
        }
    }

    /**
     * Takes an input stream and parses it into a WebSocket frame.
     *
     * @param input The incoming {@link InputStream}
     * @throws IOException if an exception occurs whilst reading the input stream
     */
    public WebSocketFrame(InputStream input) throws IOException {
        byte firstByte = (byte) input.read();
        setFinAndOpCode(firstByte);
        if (this.opcode == 2) {
            byte maskLengthByte = (byte) input.read();
            boolean masked = ((maskLengthByte & 0x80) != 0);
            int payloadLength = (byte) (0x7F & maskLengthByte);
            int byteCount = 0;
            if (payloadLength == 0X7F) {
                // 8 Byte Extended payload length
                byteCount = 8;
            } else if (payloadLength == 0X7E) {
                // 2 bytes extended payload length
                byteCount = 2;
            }

            // Decode the payload length
            if (byteCount > 0) {
                payloadLength = 0;
            }
            while (--byteCount >= 0) {
                maskLengthByte = (byte) input.read();
                payloadLength |= (maskLengthByte & 0xFF) << (8 * byteCount);
            }

            // Get the masking key
            byte[] maskingKey = null;
            if (masked) {
                maskingKey = new byte[4];
                input.read(maskingKey, 0, 4);
            }

            this.payload = new byte[payloadLength];
            int offsetIndex = 0;
            int tempLength = payloadLength;
            int bytesRead;
            while (offsetIndex != payloadLength) {
                bytesRead = input.read(this.payload, offsetIndex, tempLength);
                offsetIndex += bytesRead;
                tempLength -= bytesRead;
            }

            // De-mask if needed
            if (masked) {
                for (int i = 0; i < this.payload.length; i++) {
                    this.payload[i] ^= maskingKey[i % 4];
                }
            }
        } else if (this.opcode == 8) {
            closeFlag = true;
        } else {
            throw new IOException("Invalid Frame: Opcode: " + this.opcode);
        }
    }

    public byte getOpcode() {
        return opcode;
    }

    public boolean isFin() {
        return fin;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isCloseFlag() {
        return closeFlag;
    }

    /**
     * Sets the frames Fin flag and opcode.
     *
     * @param incomingByte
     */
    private void setFinAndOpCode(byte incomingByte) {
        this.fin = ((incomingByte & 0x80) != 0);
        // Reserved bits, unused right now.
        // boolean rsv1 = ((incomingByte & 0x40) != 0);
        // boolean rsv2 = ((incomingByte & 0x20) != 0);
        // boolean rsv3 = ((incomingByte & 0x10) != 0);
        this.opcode = (byte) (incomingByte & 0x0F);

    }

    /**
     * Encodes the this WebSocketFrame into a byte array.
     *
     * @return byte array
     */
    public byte[] encodeFrame() {
        int length = this.payload.length + frameLengthOverhead;
        // Calculating overhead
        if (this.payload.length > 65535) {
            length += 8;
        } else if (this.payload.length >= 126) {
            length += 2;
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);
        appendFinAndOpCode(buffer, this.opcode, this.fin);
        byte[] mask = generateMaskingKey();
        appendLengthAndMask(buffer, this.payload.length, mask);

        for (int i = 0; i < this.payload.length; i++) {
            buffer.put((this.payload[i] ^= mask[i % 4]));
        }

        buffer.flip();
        return buffer.array();
    }

    /**
     * Appends the Length and Mask to the buffer
     *
     * @param buffer the outgoing {@link ByteBuffer}
     * @param length the length of the frame
     * @param mask   The WebSocket Mask
     */
    public static void appendLengthAndMask(ByteBuffer buffer, int length, byte[] mask) {
        if (mask != null) {
            appendLength(buffer, length, true);
            buffer.put(mask);
        } else {
            appendLength(buffer, length, false);
        }
    }

    /**
     * Appends the Length of the payload to the buffer
     *
     * @param buffer
     * @param length
     * @param masked
     */
    private static void appendLength(ByteBuffer buffer, int length, boolean masked) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        byte b = (masked ? (byte) 0x80 : 0x00);
        if (length > 0xFFFF) {
            buffer.put((byte) (b | 0x7F));
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.put((byte) ((length >> 24) & 0xFF));
            buffer.put((byte) ((length >> 16) & 0xFF));
            buffer.put((byte) ((length >> 8) & 0xFF));
            buffer.put((byte) (length & 0xFF));
        } else if (length >= 0x7E) {
            buffer.put((byte) (b | 0x7E));
            buffer.put((byte) (length >> 8));
            buffer.put((byte) (length & 0xFF));
        } else {
            buffer.put((byte) (b | length));
        }
    }

    /**
     * Appends the Fin flag and the OpCode
     *
     * @param buffer The outgoing buffer
     * @param opcode The WebSocket OpCode
     * @param fin    if this is a final frame
     */
    public static void appendFinAndOpCode(ByteBuffer buffer, byte opcode, boolean fin) {
        byte b = 0x00;
        // Add Fin flag
        if (fin) {
            b |= 0x80;
        }
        //RSV 1,2,3 aren't important

        // Add opcode
        b |= opcode & 0x0F;
        buffer.put(b);
    }

    /**
     * Generates a random masking key
     * Nothing super secure, but enough
     * for websockets.
     *
     * @return ByteArray containing the key;
     */
    public static byte[] generateMaskingKey() {
        SecureRandom secureRandomGenerator = new SecureRandom();
        int a = secureRandomGenerator.nextInt(255);
        int b = secureRandomGenerator.nextInt(255);
        int c = secureRandomGenerator.nextInt(255);
        int d = secureRandomGenerator.nextInt(255);
        return new byte[]{(byte) a, (byte) b, (byte) c, (byte) d};
    }
}
