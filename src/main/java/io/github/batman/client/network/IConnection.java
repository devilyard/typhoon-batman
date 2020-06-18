/*
 * Copyright (c) 2019. All rights reserved.
 * IConnection.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import io.github.batman.client.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author C.
 */
public interface IConnection {

    void open() throws IOException, MqttException;

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    void close() throws IOException;

    String getServerURI();
}
