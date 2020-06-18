/*
 * Copyright (c) 2019. All rights reserved.
 * OutboundMessageHandlerFactory.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttMessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author C.
 */
public class OutboundMessageHandlerFactory {

    private static final Map<MqttMessageType, OutboundMqttMessageHandler<? extends MqttMessage>> handlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <Handler extends OutboundMqttMessageHandler<? extends MqttMessage>> Handler getHandler(MqttMessageType messageType) {
        return (Handler) handlers.get(messageType);
    }

    public static void register(MqttMessageType messageType, OutboundMqttMessageHandler<? extends MqttMessage> handler) {
        handlers.put(messageType, handler);
    }
}
