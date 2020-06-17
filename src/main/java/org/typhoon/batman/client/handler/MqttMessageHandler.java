/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler;

import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.message.MqttMessage;

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
