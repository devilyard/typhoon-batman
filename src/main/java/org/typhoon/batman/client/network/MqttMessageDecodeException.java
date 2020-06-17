/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageDecodeException.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

/**
 * @author C.
 */
public class MqttMessageDecodeException extends RuntimeException {

    public MqttMessageDecodeException() {
    }

    public MqttMessageDecodeException(String message) {
        super(message);
    }

    public MqttMessageDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttMessageDecodeException(Throwable cause) {
        super(cause);
    }

    public MqttMessageDecodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
