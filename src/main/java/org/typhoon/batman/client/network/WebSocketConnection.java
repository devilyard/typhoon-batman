/*
 * Copyright (c) 2019. All rights reserved.
 * WebSocketConnection.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import org.typhoon.batman.client.MqttException;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;

/**
 * @author C.
 */
public class WebSocketConnection extends TCPConnection implements IWebSocketConnection {

    private final String serverURI;
    private final PipedInputStream pipedInputStream;
    private WebSocketReceiver webSocketReceiver;
    private final WebSocketOutputStream webSocketOutputStream = new WebSocketOutputStream(this);

    public WebSocketConnection(String serverURI, String host, int port, SocketFactory factory) {
        super(host, port, factory);
        this.serverURI = serverURI;
        this.pipedInputStream = new PipedInputStream();
    }

    @Override
    public void open() throws IOException, MqttException {
        super.open();
        WebSocketHandshake handshake = new WebSocketHandshake(getSocketInputStream(), getSocketOutputStream(), serverURI, host, port);
        handshake.execute();
        this.webSocketReceiver = new WebSocketReceiver(getSocketInputStream(), pipedInputStream);
        webSocketReceiver.start("webSocketReceiver");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return pipedInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return webSocketOutputStream;
    }

    @Override
    public OutputStream getSocketOutputStream() throws IOException {
        return super.getOutputStream();
    }

    @Override
    public InputStream getSocketInputStream() throws IOException {
        return super.getInputStream();
    }

    /**
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        WebSocketFrame frame = new WebSocketFrame((byte) 0x08, true, new byte[0]);
        byte[] rawFrame = frame.encodeFrame();
        getSocketOutputStream().write(rawFrame);
        getSocketOutputStream().flush();

        if (webSocketReceiver != null) {
            webSocketReceiver.stop();
        }
        super.close();
    }

    public String getServerURI() {
        return serverURI;
    }
}
