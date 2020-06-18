/*
 * Copyright (c) 2019. All rights reserved.
 * MqttPubAckMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.handler.outbound.Qos1PublishPromise;
import io.github.batman.client.message.MqttPubAckMessage;
import io.github.batman.client.message.MqttPublishMessage;

/**
 * @author C.
 */
public class MqttPubAckMessageHandler implements InboundMqttMessageHandler<MqttPubAckMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPubAckMessageHandler.class);


    @Override
    public void handle(MqttPubAckMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing PUBACK message, packetId: {}", packetId);
        }
        if (context.getSessionStore().inFlightAck(packetId)) {
            MqttPublishMessage publishMessage = context.getSessionStore().remove(packetId);
            Qos1PublishPromise promise = (Qos1PublishPromise) context.getPromiseKeeper().remove(message);
            if (promise != null) {
                promise.setResult(message);
                context.getMessageTransporter().notifyPublished(publishMessage);
            }
        }
    }
}
