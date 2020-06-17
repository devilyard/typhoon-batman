/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubRecMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.message.MqttPacketIdVariableHeader;
import org.typhoon.batman.client.message.MqttPubRecMessage;
import org.typhoon.batman.client.message.MqttPubRelMessage;

import static org.typhoon.batman.client.handler.MqttMessageHandlerHelper.appendToPendingZone;

/**
 * @author C.
 */
public class MqttPubRecMessageHandler implements InboundMqttMessageHandler<MqttPubRecMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPubRecMessageHandler.class);

    @Override
    public void handle(MqttPubRecMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing PUBREC message, packetId: {}", packetId);
        }
        try {
            context.getSessionStore().recReceived(packetId);
        } catch (MqttException e) {
            LOGGER.error("Cannot acknowledge message: {}", packetId, e);
            return;
        }

        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
        appendToPendingZone(pubRelMessage, context);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending PUBREL message, packetId: {}", packetId);
        }
    }
}
