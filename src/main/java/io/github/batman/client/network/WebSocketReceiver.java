/*
 * Copyright (c) 2019. All rights reserved.
 * WebSocketReceiver.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class WebSocketReceiver implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketReceiver.class);

    private boolean running = false;
    private boolean stopping = false;
    private final byte[] lifecycle = new byte[0];
    private final InputStream input;
    private Thread receiverThread = null;
    private final PipedOutputStream pipedOutputStream;

    public WebSocketReceiver(InputStream input, PipedInputStream pipedInputStream) throws IOException {
        this.input = input;
        this.pipedOutputStream = new PipedOutputStream();
        pipedInputStream.connect(pipedOutputStream);
    }

    /**
     * Starts up the WebSocketReceiver's thread
     *
     * @param threadName The name of the thread
     */
    public void start(String threadName) {
        if (!running) {
            synchronized (lifecycle) {
                if (!running) {
                    running = true;
                    receiverThread = new Thread(this, threadName);
                    receiverThread.start();

                    LOGGER.info("WebSocket receiver is started.");
                }
            }
        }
    }

    /**
     * Stops this WebSocketReceiver's thread.
     * This call will block.
     */
    public void stop() {
        stopping = true;
        boolean closed = false;
        if (running) {
            synchronized (lifecycle) {
                if (running) {
                    running = false;
                    closed = true;
                    closeOutputStream();

                    LOGGER.info("WebSocket receiver is stopped.");
                }
            }
        }
        if (closed && !Thread.currentThread().equals(receiverThread)) {
            try {
                // Wait for the thread to finish
                // This must not happen in the synchronized block, otherwise we can deadlock ourselves!
                receiverThread.join();
            } catch (InterruptedException ignore) {
            }
        }
        receiverThread = null;
    }

    public void run() {
        while (running && (input != null)) {
            try {
                WebSocketFrame incomingFrame = new WebSocketFrame(input);
                if (!incomingFrame.isCloseFlag()) {
                    pipedOutputStream.write(incomingFrame.getPayload());
                    pipedOutputStream.flush();
                } else {
                    if (!stopping) {
                        throw new IOException("Server sent a WebSocket Frame with the Stop OpCode");
                    }
                }
            } catch (IOException ex) {
                this.stop();
            }
        }
    }

    private void closeOutputStream() {
        try {
            pipedOutputStream.close();
        } catch (IOException ignore) {
        }
    }

}
