/*
 * Copyright (c) 2019. All rights reserved.
 * MqttOutputStream.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.client.message.MqttMessage;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author C.
 */
public class MqttOutputStream extends OutputStream {

    private final BufferedOutputStream outputStream;

    public MqttOutputStream(OutputStream outputStream) {
        if (outputStream instanceof BufferedOutputStream) {
            this.outputStream = (BufferedOutputStream) outputStream;
        } else {
            this.outputStream = new BufferedOutputStream(outputStream);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    /**
     *
     * @param message
     * @throws IOException
     */
    public void write(MqttMessage message) throws IOException {
        byte[] data = MqttEncoder.encode(message);
        int chunkSize = 1024;
        int offset = 0;
        while (offset < data.length) {
            int length = Math.min(chunkSize, data.length - offset);
            outputStream.write(data, offset, length);
            offset += chunkSize;
        }
    }
}
