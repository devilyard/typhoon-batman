/*
 * Copyright (c) 2019. All rights reserved.
 * MqttUnsubAckMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.handler.outbound.UnsubscribePromiseImpl;
import io.github.batman.client.message.MqttUnsubAckMessage;

/**
 * @author C.
 */
public class MqttUnsubAckMessageHandler implements InboundMqttMessageHandler<MqttUnsubAckMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttUnsubAckMessageHandler.class);

    @Override
    public void handle(MqttUnsubAckMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing UNSUBACK message, packetId: {}", packetId);
        }
        context.getSessionStore().releasePacketId(packetId);
        UnsubscribePromiseImpl promise = (UnsubscribePromiseImpl) context.getPromiseKeeper().remove(message);
        promise.setResult(message);
    }
}
