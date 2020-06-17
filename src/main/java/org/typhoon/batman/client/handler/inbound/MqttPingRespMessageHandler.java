/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPingRespMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.message.MqttPingRespMessage;

/**
 * @author C.
 */
public class MqttPingRespMessageHandler implements InboundMqttMessageHandler<MqttPingRespMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPingRespMessageHandler.class);

    @Override
    public void handle(MqttPingRespMessage message, ClientContext context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PONG");
        }
    }
}
