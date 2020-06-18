/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler;

import io.github.batman.client.ClientContext;
import io.github.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public interface MqttMessageHandler<Message extends MqttMessage> {

    /**
     * Handles message outbound logic.
     *
     * @param message the message to send
     * @param context client context
     */
     void handle(Message message, ClientContext context);
}
