/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnectMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.message.MqttConnectMessage;

/**
 * @author C.
 */
public class MqttConnectMessageHandler implements OutboundMqttMessageHandler<MqttConnectMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectMessageHandler.class);

    @Override
    public void handle(MqttConnectMessage message, ClientContext context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing CONNECT message, clientId: {}", message.payload().clientIdentifier());
        }
        context.getSessionStore().prependToOutboundZone(message);
    }
}
