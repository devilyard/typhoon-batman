/*
 * Copyright (c) 2019. All rights reserved.
 * DisconnectPromiseImpl.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.message.MqttDisconnectMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
public class DisconnectPromiseImpl extends AbstractActionPromise<MqttDisconnectMessage> implements DisconnectPromise {

    public DisconnectPromiseImpl(ExecutorService executorService) {
        super(executorService);
    }
}
