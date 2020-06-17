/*
 * Copyright (c) 2019. All rights reserved.
 * WebSocketOutputStream.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author C.
 */
public class WebSocketOutputStream extends ByteArrayOutputStream {

    private final IWebSocketConnection connection;

    public WebSocketOutputStream(IWebSocketConnection connection) {
        this.connection = connection;
    }

    @Override
    public void flush() throws IOException {
        byte[] bytes = toByteArray();
        reset();
        WebSocketFrame frame = new WebSocketFrame((byte) 0x02, true, bytes);
        byte[] rawFrame = frame.encodeFrame();
        OutputStream outputStream = connection.getSocketOutputStream();
        outputStream.write(rawFrame);
        outputStream.flush();
    }
}
