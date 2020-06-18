/*
 * Copyright (c) 2019. All rights reserved.
 * MqttUnsubscribeMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.client.ClientContext;
import io.github.batman.client.SubscribedMessageNotifier;
import io.github.batman.client.Topic;
import io.github.batman.client.message.MqttUnsubscribeMessage;

import java.util.List;

import static io.github.batman.client.handler.MqttMessageHandlerHelper.appendToPendingZone;

/**
 * @author C.
 */
public class MqttUnsubscribeMessageHandler implements OutboundMqttMessageHandler<MqttUnsubscribeMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttUnsubscribeMessageHandler.class);

    @Override
    public void handle(MqttUnsubscribeMessage message, ClientContext context) {
        if (context.getClientSession().isQuiescent()) {
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing UNSUBSCRIBE message, packetId: {}", message.variableHeader().packetId());
        }
        appendToPendingZone(message, context);
        List<String> topics = message.payload().topics();
        if (topics != null && !topics.isEmpty()) {
            SubscribedMessageNotifier messageNotifier = context.getMessageNotifier();
            for (String topicFilter : topics) {
                messageNotifier.removeListener(new Topic(topicFilter));
            }
        }
    }
}
