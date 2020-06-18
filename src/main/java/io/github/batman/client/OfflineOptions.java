/*
 * Copyright (c) 2019. All rights reserved.
 * OfflineOptions.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

/**
 * @author C.
 */
public class OfflineOptions {

    private static final int DEFAULT_OFFLINE_BUFFER_SIZE = 5000;
    private static final boolean DEFAULT_BEAR_MODE = false;

    private int maxBufferSize = DEFAULT_OFFLINE_BUFFER_SIZE;
    private boolean bearMode = DEFAULT_BEAR_MODE;

    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    public boolean isBearMode() {
        return bearMode;
    }

    /**
     * Sets the bear mode switch, with bear mode is turn on, the oldest messages
     * will be deleted when buffer is full.
     *
     * @param bearMode true or false for the bear mode
     */
    public void setBearMode(boolean bearMode) {
        this.bearMode = bearMode;
    }
}
