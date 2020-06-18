/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPingReqMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.message.MqttPingReqMessage;

/**
 * @author C.
 */
public class MqttPingReqMessageHandler implements OutboundMqttMessageHandler<MqttPingReqMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPingReqMessageHandler.class);

    @Override
    public void handle(MqttPingReqMessage message, ClientContext context) {
        if (context.getClientSession().isQuiescent()) {
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PING");
        }
        if (context.getClientSession().isConnected()) {
            context.getSessionStore().prependToOutboundZone(message);
        }
    }
}
