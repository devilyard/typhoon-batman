/*
 * Copyright (c) 2019. All rights reserved.
 * ConnectionFactory.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.client.MqttException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author C.
 */
public class ConnectionFactory {

    private static final int URI_TYPE_TCP = 0;
    private static final int URI_TYPE_SSL = 1;
    private static final int URI_TYPE_WS = 2;
    private static final int URI_TYPE_WSS = 3;

    /**
     * @param address
     * @param options
     * @return
     * @throws MqttException
     */
    public static IConnection getConnection(String address, MqttConnectOptions options) throws MqttException {
        SocketFactory factory = options.getSocketFactory();
        int serverURIType = validateURI(address);
        URI uri = normalizeAddress(address);
        String host = uri.getHost();
        int port = uri.getPort(); // -1 if not defined

        IConnection connection = null;
        switch (serverURIType) {
            case URI_TYPE_TCP:
                connection = createTCPConnection(host, port, options, factory);
                break;
            case URI_TYPE_SSL:
                connection = createSSLTCPConnection(host, port, options, factory);
                break;
            case URI_TYPE_WS:
                connection = createWebSocketConnection(address, host, port, options, factory);
                break;
            case URI_TYPE_WSS:
                connection = createWebSocketSecureConnection(address, host, port, options, factory);
                break;
            default:
        }
        return connection;
    }

    private static IConnection createTCPConnection(String host, int port, MqttConnectOptions options, SocketFactory socketFactory) throws MqttException {
        if (port == -1) {
            port = 7088;
        }
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        } else if (socketFactory instanceof SSLSocketFactory) {
            throw new MqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }
        TCPConnection connection = new TCPConnection(host, port, socketFactory);
        connection.setConnectTimeout(options.getConnectionTimeout());
        return connection;
    }

    private static IConnection createSSLTCPConnection(String host, int port, MqttConnectOptions options, SocketFactory socketFactory) throws MqttException {
        if (port == -1) {
            port = 7083;
        }
        SSLSocketFactoryFactory sslSocketFactoryFactory = null;
        if (socketFactory == null) {
            sslSocketFactoryFactory = getSSLSocketFactoryFactory(options);
            socketFactory = sslSocketFactoryFactory.createSocketFactory();
        } else if (!(socketFactory instanceof SSLSocketFactory)) {
            throw new MqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }

        SSLTCPConnection connection = new SSLTCPConnection(host, port, (SSLSocketFactory) socketFactory);
        connection.setSSLHandshakeTimeout(options.getConnectionTimeout());
        connection.setSSLHostnameVerifier(options.getSSLHostnameVerifier());

        if (sslSocketFactoryFactory != null) {
            String[] enabledCiphers = sslSocketFactoryFactory.getEnabledCipherSuites();
            if (enabledCiphers != null) {
                connection.setEnabledCipherSuites(enabledCiphers);
            }
        }
        return connection;
    }

    private static IConnection createWebSocketConnection(String address, String host, int port, MqttConnectOptions options, SocketFactory socketFactory) throws MqttException {
        if (port == -1) {
            port = 7086;
        }
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        } else if (socketFactory instanceof SSLSocketFactory) {
            throw new MqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }
        WebSocketConnection connection = new WebSocketConnection(address, host, port, socketFactory);
        connection.setConnectTimeout(options.getConnectionTimeout());
        return connection;
    }

    private static IConnection createWebSocketSecureConnection(String address, String host, int port, MqttConnectOptions options, SocketFactory socketFactory) throws MqttException {
        if (port == -1) {
            port = 7443;
        }
        SSLSocketFactoryFactory wssFactoryFactory = null;
        if (socketFactory == null) {
            wssFactoryFactory = getSSLSocketFactoryFactory(options);
            socketFactory = wssFactoryFactory.createSocketFactory();
        } else if (!(socketFactory instanceof SSLSocketFactory)) {
            throw new MqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }

        WebSocketSecureConnection connection = new WebSocketSecureConnection(address, host, port, (SSLSocketFactory) socketFactory);
        connection.setSSLHandshakeTimeout(options.getConnectionTimeout());
        if (wssFactoryFactory != null) {
            String[] enabledCiphers = wssFactoryFactory.getEnabledCipherSuites();
            if (enabledCiphers != null) {
                connection.setEnabledCipherSuites(enabledCiphers);
            }
        }
        return connection;
    }

    /**
     * Validate a URI
     *
     * @param srvURI The Server URI
     * @return the URI type
     */
    public static int validateURI(String srvURI) {
        try {
            URI vURI = new URI(srvURI);
            if ("ws".equals(vURI.getScheme())) {
                return URI_TYPE_WS;
            } else if ("wss".equals(vURI.getScheme())) {
                return URI_TYPE_WSS;
            }

            if (vURI.getPath() != null && !vURI.getPath().isEmpty()) {
                throw new IllegalArgumentException(srvURI);
            }

            if ("tcp".equals(vURI.getScheme())) {
                return URI_TYPE_TCP;
            } else if ("ssl".equals(vURI.getScheme())) {
                return URI_TYPE_SSL;
            }

            throw new IllegalArgumentException(srvURI);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(srvURI);
        }
    }

    private static URI normalizeAddress(String address) throws MqttException {
        URI uri;
        try {
            uri = new URI(address);
            // If the returned uri contains no host and the address contains underscores,
            // then it's likely that Java did not parse the URI
            if (uri.getHost() == null && address.contains("_")) {
                try {
                    final Field hostField = URI.class.getDeclaredField("host");
                    hostField.setAccessible(true);
                    // Get everything after the scheme://
                    String shortAddress = address.substring(uri.getScheme().length() + 3);
                    hostField.set(uri, getHostName(shortAddress));

                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    throw new MqttException(e.getCause());
                }

            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URI: " + address + ", " + e.getMessage());
        }
        return uri;
    }

    private static SSLSocketFactoryFactory getSSLSocketFactoryFactory(MqttConnectOptions options) throws MqttException {
        SSLSocketFactoryFactory socketFactoryFactory = new SSLSocketFactoryFactory();
        Properties sslClientProps = options.getSSLProperties();
        if (null != sslClientProps) {
            socketFactoryFactory.initialize(sslClientProps);
        }
        return socketFactoryFactory;
    }

    private static String getHostName(String uri) {
        int portIndex = uri.indexOf(':');
        if (portIndex == -1) {
            portIndex = uri.indexOf('/');
        }
        if (portIndex == -1) {
            portIndex = uri.length();
        }
        return uri.substring(0, portIndex);
    }
}
