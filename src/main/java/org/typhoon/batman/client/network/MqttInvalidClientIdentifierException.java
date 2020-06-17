/*
 * Copyright (c) 2019. All rights reserved.
 * MqttInvalidClientIdentifierException.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

/**
 * @author C.
 */
public class MqttInvalidClientIdentifierException extends RuntimeException {

    public MqttInvalidClientIdentifierException() {
    }

    public MqttInvalidClientIdentifierException(String message) {
        super(message);
    }

    public MqttInvalidClientIdentifierException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttInvalidClientIdentifierException(Throwable cause) {
        super(cause);
    }

    public MqttInvalidClientIdentifierException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
