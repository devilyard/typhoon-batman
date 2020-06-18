/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubscribeMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.message.MqttSubscribeMessage;

import static io.github.batman.client.handler.MqttMessageHandlerHelper.appendToPendingZone;

/**
 * @author C.
 */
public class MqttSubscribeMessageHandler implements OutboundMqttMessageHandler<MqttSubscribeMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

    @Override
    public void handle(MqttSubscribeMessage message, ClientContext context) {
        if (context.getClientSession().isQuiescent()) {
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing SUBSCRIBE message, packetId: {}", message.variableHeader().packetId());
        }
        appendToPendingZone(message, context);
    }
}
