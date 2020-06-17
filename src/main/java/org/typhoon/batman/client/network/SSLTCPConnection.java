/*
 * Copyright (c) 2019. All rights reserved.
 * SSLTCPConnection.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import org.typhoon.batman.client.MqttException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

/**
 * @author C.
 */
public class SSLTCPConnection extends TCPConnection {

    private String[] enabledCipherSuites;
    private int handshakeTimeoutSecs;
    private HostnameVerifier hostnameVerifier;

    public SSLTCPConnection(String host, int port, SSLSocketFactory socketFactory) {
        super(host, port, socketFactory);
    }

    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
        if (socket != null && enabledCipherSuites != null) {
            ((SSLSocket) socket).setEnabledCipherSuites(enabledCipherSuites);
        }
    }

    public void setSSLHandshakeTimeout(int timeout) {
        super.setConnectTimeout(timeout);
        this.handshakeTimeoutSecs = timeout;
    }

    public HostnameVerifier getSSLHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public void open() throws IOException, MqttException {
        super.open();
        setEnabledCipherSuites(enabledCipherSuites);
        int soTimeout = socket.getSoTimeout();
        // Set a timeout to avoid the SSL handshake being blocked indefinitely
        socket.setSoTimeout(this.handshakeTimeoutSecs * 1000);
        ((SSLSocket) socket).startHandshake();
        if (hostnameVerifier != null) {
            SSLSession session = ((SSLSocket) socket).getSession();
            hostnameVerifier.verify(host, session);
        }
        socket.setSoTimeout(soTimeout);
    }

    public String getServerURI() {
        return "ssl://" + host + ":" + port;
    }
}
