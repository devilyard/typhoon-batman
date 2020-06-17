/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnAckVariableHeader.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

/**
 * @author C.
 */
public final class MqttConnAckVariableHeader implements MqttVariableHeader {

    private final MqttConnectReturnCode connectReturnCode;
    private final boolean sessionPresent;

    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
    }

    public MqttConnectReturnCode connectReturnCode() {
        return connectReturnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[connectReturnCode=" + connectReturnCode + ", sessionPresent=" + sessionPresent + ']';
    }
}