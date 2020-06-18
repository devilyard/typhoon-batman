/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubCompMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.handler.outbound.Qos2PublishPromise;
import io.github.batman.client.message.MqttPubCompMessage;
import io.github.batman.client.message.MqttPublishMessage;

/**
 * @author C.
 */
public class MqttPubCompMessageHandler implements InboundMqttMessageHandler<MqttPubCompMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPubCompMessageHandler.class);

    @Override
    public void handle(MqttPubCompMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing PUBCOMP message, packetId: {}", packetId);
        }
        context.getSessionStore().inFlightAck(packetId);
        MqttPublishMessage publishMessage = context.getSessionStore().remove(packetId);
        Qos2PublishPromise promise = (Qos2PublishPromise) context.getPromiseKeeper().remove(message);
        if (promise != null) {
            promise.setResult(message);
        }
        context.getMessageTransporter().notifyPublished(publishMessage);
    }
}
