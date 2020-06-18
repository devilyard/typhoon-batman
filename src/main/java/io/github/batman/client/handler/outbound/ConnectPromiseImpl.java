/*
 * Copyright (c) 2019. All rights reserved.
 * ConnectPromiseImpl.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.client.message.MqttConnAckMessage;
import io.github.batman.client.message.MqttVersion;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
final public class ConnectPromiseImpl extends AbstractActionPromise<MqttConnAckMessage> implements ConnectPromise {

    private final String username;
    private final byte[] password;
    private final String clientId;
    private final MqttVersion mqttVersion;

    public ConnectPromiseImpl(String username, byte[] password, String clientId, MqttVersion mqttVersion, ExecutorService executorService) {
        super(executorService);
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.mqttVersion = mqttVersion;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public byte[] getPassword() {
        return password;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }
}