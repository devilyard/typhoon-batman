/*
 * Copyright (c) 2019. All rights reserved.
 * TCPConnection.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.client.MqttException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author C.
 */
public class TCPConnection implements IConnection {

    protected Socket socket;
    private final SocketFactory factory;
    protected String host;
    protected int port;
    private int conTimeout;

    public TCPConnection(String host, int port, SocketFactory factory) {
        this.host = host;
        this.port = port;
        this.factory = factory;
    }

    @Override
    public void open() throws IOException, MqttException {
        try {
            SocketAddress sockAddr = new InetSocketAddress(host, port);
            if (factory instanceof SSLSocketFactory) {
                // SNI support
                Socket socketForSNI = new Socket();
                socketForSNI.connect(sockAddr, conTimeout * 1000);
                socket = ((SSLSocketFactory) factory).createSocket(socketForSNI, host, port, true);
            } else {
                socket = factory.createSocket();
                socket.connect(sockAddr, conTimeout * 1000);
            }
        } catch (ConnectException ex) {
            throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket == null ? null : socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket == null ? null : socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        if (socket != null && socket.isConnected()) {
            socket.shutdownInput();
            socket.close();
        }
    }

    public void setConnectTimeout(int timeout) {
        this.conTimeout = timeout;
    }

    @Override
    public String getServerURI() {
        return "tcp://" + host + ":" + port;
    }
}
