/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubRelMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.SubscribedMessageNotifier;
import org.typhoon.batman.client.message.MqttPacketIdVariableHeader;
import org.typhoon.batman.client.message.MqttPubCompMessage;
import org.typhoon.batman.client.message.MqttPubRelMessage;
import org.typhoon.batman.client.message.MqttPublishMessage;

import static org.typhoon.batman.client.handler.MqttMessageHandlerHelper.appendToPendingZone;

/**
 * @author C.
 */
public class MqttPubRelMessageHandler implements InboundMqttMessageHandler<MqttPubRelMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPubRelMessageHandler.class);

    @Override
    public void handle(MqttPubRelMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing PUBREL message, packetId: {}", packetId);
        }
        MqttPublishMessage publishMessage = context.getClientSession().inFlightAck(packetId);
        if (publishMessage != null) {
            SubscribedMessageNotifier messageNotifier = context.getMessageNotifier();
            messageNotifier.notifyMessage(publishMessage);
        }
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        MqttPubCompMessage pubCompMessage = new MqttPubCompMessage(variableHeader);
        appendToPendingZone(pubCompMessage, context);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending PUBCOMP message, packetId: {}", packetId);
        }
    }

}
