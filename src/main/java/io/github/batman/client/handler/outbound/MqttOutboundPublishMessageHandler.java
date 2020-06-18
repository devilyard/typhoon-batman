/*
 * Copyright (c) 2019. All rights reserved.
 * MqttOutboundPublishMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.MqttQoS;
import io.github.batman.client.ClientContext;
import io.github.batman.client.MqttException;
import io.github.batman.client.message.MqttPublishMessage;

import static io.github.batman.client.handler.MqttMessageHandlerHelper.appendToPendingZone;

/**
 * @author C.
 */
public class MqttOutboundPublishMessageHandler implements OutboundMqttMessageHandler<MqttPublishMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttOutboundPublishMessageHandler.class);

    @Override
    public void handle(MqttPublishMessage message, ClientContext context) {
        if (context.getClientSession().isQuiescent()) {
            return;
        }
        MqttQoS qos = message.fixedHeader().qosLevel();
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing PUBLISH message with qos level: {}, topic: {}, packetId: {}", qos, message.variableHeader().topicName(), packetId);
        }

        if (qos.compareTo(MqttQoS.AT_MOST_ONCE) > 0) {
            context.getSessionStore().store(message);
        }
        appendToPendingZone(message, context);
    }

    @Override
    public void onMessageSent(MqttPublishMessage message, ClientContext clientContext) {
        MqttQoS qoS = message.fixedHeader().qosLevel();
        int packetId = message.variableHeader().packetId();
        if (qoS == MqttQoS.AT_MOST_ONCE) {
            Qos0PublishPromise promise = (Qos0PublishPromise) clientContext.getPromiseKeeper().remove(message);
            if (promise != null) {
                promise.setResult(null);
            }
            clientContext.getSessionStore().inFlightAck(packetId);
            clientContext.getMessageTransporter().notifyPublished(message);
        } else if (qoS == MqttQoS.AT_LEAST_ONCE) {
            try {
                clientContext.getSessionStore().inFlight(packetId);
            } catch (MqttException e) {
                LOGGER.error("Cannot add message: {} to in flight zone", packetId, e);
                return;
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Waiting for PUBACK, packetId: {}", packetId);
            }
        } else if (qoS == MqttQoS.EXACTLY_ONCE) {
            clientContext.getSessionStore().waitingForRec(packetId);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Waiting for PUBREC, packetId: {}", packetId);
            }
        }
    }
}
