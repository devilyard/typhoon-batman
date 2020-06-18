/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubAckMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.batman.MqttQoS;
import io.github.batman.client.ClientContext;
import io.github.batman.client.MessageListener;
import io.github.batman.client.SubscribedMessageNotifier;
import io.github.batman.client.Topic;
import io.github.batman.client.handler.outbound.SubscribePromiseImpl;
import io.github.batman.client.message.MqttSubAckMessage;

import java.util.Map;

/**
 * @author C.
 */
public class MqttSubAckMessageHandler implements InboundMqttMessageHandler<MqttSubAckMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSubAckMessageHandler.class);

    @Override
    public void handle(MqttSubAckMessage message, ClientContext context) {
        int packetId = message.variableHeader().packetId();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing SUBACK message, packetId: {}", packetId);
        }
        context.getSessionStore().releasePacketId(packetId);
        SubscribePromiseImpl promise = (SubscribePromiseImpl) context.getPromiseKeeper().remove(message);
        if (promise != null) {
            promise.setResult(message);
            Map<String, Map<MqttQoS, MessageListener>> listeners = promise.getSubscriptions();
            if (listeners != null && !listeners.isEmpty()) {
                SubscribedMessageNotifier messageNotifier = context.getMessageNotifier();
                for (Map.Entry<String, Map<MqttQoS, MessageListener>> entry : listeners.entrySet()) {
                    String topic = entry.getKey();
                    Topic topicFilter = new Topic(topic);
                    Map<MqttQoS, MessageListener> map = entry.getValue();
                    for (Map.Entry<MqttQoS, MessageListener> mm : map.entrySet()) {
                        messageNotifier.addListener(topicFilter, mm.getValue());
                    }
                }
            }
        }
    }
}
