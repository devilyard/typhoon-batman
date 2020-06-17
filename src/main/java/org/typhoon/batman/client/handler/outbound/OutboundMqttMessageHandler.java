/*
 * Copyright (c) 2019. All rights reserved.
 * OutboundMqttMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.handler.MqttMessageHandler;
import org.typhoon.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public interface OutboundMqttMessageHandler<Message extends MqttMessage> extends MqttMessageHandler<Message> {

    /**
     * Fired when message is sent.
     *
     * @param message       message that is just sent
     * @param clientContext client context
     */
    default void onMessageSent(Message message, ClientContext clientContext) {
    }
}
