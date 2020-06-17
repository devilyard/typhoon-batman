/*
 * Copyright (c) 2019. All rights reserved.
 * WebSocketHandshake.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Helper class to execute a WebSocket Handshake.
 */
public class WebSocketHandshake {

    // Do not change: https://tools.ietf.org/html/rfc6455#section-1.3
    private static final String ACCEPT_SALT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final String SHA1_PROTOCOL = "SHA1";
    private static final String HTTP_HEADER_SEC_WEBSOCKET_ACCEPT = "sec-websocket-accept";
    private static final String HTTP_HEADER_UPGRADE = "upgrade";
    private static final String HTTP_HEADER_UPGRADE_WEBSOCKET = "websocket";
    private static final String LINE_SEPARATOR = "\r\n";

    private static final String HTTP_HEADER_CONNECTION = "connection";
    private static final String HTTP_HEADER_CONNECTION_VALUE = "upgrade";
    private static final String HTTP_HEADER_SEC_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";

    private final InputStream input;
    private final OutputStream output;
    private final String uri;
    private final String host;
    private final int port;

    public WebSocketHandshake(InputStream input, OutputStream output, String uri, String host, int port) {
        this.input = input;
        this.output = output;
        this.uri = uri;
        this.host = host;
        this.port = port;
    }

    /**
     * Executes a Websocket Handshake.
     * Will throw an IOException if the handshake fails
     *
     * @throws IOException thrown if an exception occurs during the handshake
     */
    public void execute() throws IOException {
        byte[] key = new byte[16];
        System.arraycopy(UUID.randomUUID().toString().getBytes(), 0, key, 0, 16);
        String b64Key = Base64.getEncoder().encodeToString(key);
        sendHandshakeRequest(b64Key);
        receiveHandshakeResponse(b64Key);
    }

    /**
     * Builds and sends the HTTP Header GET Request
     * for the socket.
     *
     * @param key Base64 encoded key
     */
    private void sendHandshakeRequest(String key) {
        try {
            String path = "/";
            URI srvUri = new URI(uri);
            if (srvUri.getRawPath() != null && !srvUri.getRawPath().isEmpty()) {
                path = srvUri.getRawPath();
            }
            if (srvUri.getRawQuery() != null && !srvUri.getRawQuery().isEmpty()) {
                path += "?" + srvUri.getRawQuery();
            }

            PrintWriter pw = new PrintWriter(output);
            print(pw, "GET ", path, " HTTP/1.1");
            if (port != 80 && port != 443) {
                print(pw, "Host: ", host, ":", String.valueOf(port));
            } else {
                print(pw, "Host: ", host);
            }

            print(pw, "Upgrade: websocket");
            print(pw, "Connection: Upgrade");
            print(pw, "Sec-WebSocket-Key: ", key);
            print(pw, "Sec-WebSocket-Protocol: mqtt");
            print(pw, "Sec-WebSocket-Version: 13");

            String userInfo = srvUri.getUserInfo();
            if (userInfo != null) {
                print(pw, "Authorization: Basic ", Base64.getEncoder().encodeToString(userInfo.getBytes(StandardCharsets.UTF_8)));
            }

            pw.print(LINE_SEPARATOR);
            pw.flush();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private void print(PrintWriter pw, String... content) {
        if (content != null && content.length > 0) {
            for (String s : content) {
                pw.print(s);
            }
            pw.print(LINE_SEPARATOR);
        }
    }

    /**
     * Receives the Handshake response and verifies that it is valid.
     *
     * @param key Base64 encoded key
     * @throws IOException
     */
    private void receiveHandshakeResponse(String key) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        List<String> responseLines = new ArrayList<>();
        String line = in.readLine();
        if (line == null) {
            throw new IOException("WebSocket Response header: Invalid response from Server, it may not support WebSocket.");
        }
        if (line.isEmpty() || !"101".equals(line.substring(9, 12))) {
            throw new IOException("WebSocket respond with non 101 status from server, it may not support WebSocket.");
        }

        while (!(line = in.readLine()).isEmpty()) {
            responseLines.add(line);
        }
        Map<String, String> headerMap = getHeaders(responseLines);

        String connectionHeader = (String) headerMap.get(HTTP_HEADER_CONNECTION);
        if (connectionHeader == null || connectionHeader.equalsIgnoreCase(HTTP_HEADER_CONNECTION_VALUE)) {
            throw new IOException("WebSocket Response header: Incorrect Connection.");
        }

        String upgradeHeader = (String) headerMap.get(HTTP_HEADER_UPGRADE);
        if (upgradeHeader == null || !upgradeHeader.toLowerCase().contains(HTTP_HEADER_UPGRADE_WEBSOCKET)) {
            throw new IOException("WebSocket Response header: Incorrect Upgrade.");
        }

        String secWebsocketProtocolHeader = (String) headerMap.get(HTTP_HEADER_SEC_WEBSOCKET_PROTOCOL);
        if (secWebsocketProtocolHeader == null) {
            throw new IOException("WebSocket Response header: empty Sec-WebSocket-Protocol.");
        }

        if (!headerMap.containsKey(HTTP_HEADER_SEC_WEBSOCKET_ACCEPT)) {
            throw new IOException("WebSocket Response header: Missing Sec-WebSocket-Accept.");
        }

        try {
            verifyWebSocketKey(key, (String) headerMap.get(HTTP_HEADER_SEC_WEBSOCKET_ACCEPT));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        } catch (HandshakeFailedException e) {
            throw new IOException("WebSocket Response header: Incorrect Sec-WebSocket-Key.");
        }
    }

    /**
     * Returns a HashMap of HTTP headers
     *
     * @param headers List<String> of headers
     * @return A HashMap<String, String> of the headers
     */
    private Map<String, String> getHeaders(List<String> headers) {
        Map<String, String> headerMap = new HashMap<>();
        for (String header : headers) {
            String[] pair = header.split(":");
            headerMap.put(pair[0].toLowerCase(), pair[1]);
        }
        return headerMap;
    }

    /**
     * Verifies that the Accept key provided is correctly built from the
     * original key sent.
     *
     * @param key
     * @param accept
     * @throws NoSuchAlgorithmException
     * @throws HandshakeFailedException
     */
    private void verifyWebSocketKey(String key, String accept) throws NoSuchAlgorithmException, HandshakeFailedException {
        // We build up the accept in the same way the server should
        // then we check that the response is the same.
        byte[] sha1Bytes = sha1(key + ACCEPT_SALT);
        String encodedSha1Bytes = Base64.getEncoder().encodeToString(sha1Bytes).trim();
        if (!encodedSha1Bytes.equals(accept.trim())) {
            throw new HandshakeFailedException();
        }
    }

    /**
     * Returns the sha1 byte array of the provided string.
     *
     * @param input
     * @return
     * @throws NoSuchAlgorithmException
     */
    private byte[] sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance(SHA1_PROTOCOL);
        return mDigest.digest(input.getBytes());
    }

}
