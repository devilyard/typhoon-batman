/*
 * Copyright (c) 2019. All rights reserved.
 * InboundMessageHandlerFactory.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.typhoon.batman.client.message.MqttMessage;
import org.typhoon.batman.client.message.MqttMessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author C.
 */
public class InboundMessageHandlerFactory {

    private static final Map<MqttMessageType, InboundMqttMessageHandler<? extends MqttMessage>> handlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <Handler extends InboundMqttMessageHandler<? extends MqttMessage>> Handler getHandler(MqttMessageType messageType) {
        return (Handler) handlers.get(messageType);
    }

    public static void register(MqttMessageType messageType, InboundMqttMessageHandler<? extends MqttMessage> handler) {
        handlers.put(messageType, handler);
    }
}
