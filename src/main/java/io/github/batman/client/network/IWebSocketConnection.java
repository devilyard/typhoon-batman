/*
 * Copyright (c) 2019. All rights reserved.
 * IWebSocketConnection.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author C.
 */
public interface IWebSocketConnection extends IConnection {

    OutputStream getSocketOutputStream() throws IOException;

    InputStream getSocketInputStream() throws IOException;
}
