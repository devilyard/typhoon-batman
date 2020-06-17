/*
 * Copyright (c) 2019. All rights reserved.
 * MqttMessageHandlerHelper.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public abstract class MqttMessageHandlerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageHandlerHelper.class);

    public static void appendToPendingZone(MqttMessage message, ClientContext context) {
        if (context.getClientSession().isConnected()) {
            context.getSessionStore().appendToOutboundZone(message);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Add message to offline zone: {}", message);
            }
            try {
                context.getSessionStore().appendToOfflineZone(message);
            } catch (MqttException e) {
                LOGGER.error("Cannot obtain offline message: {}", message, e);
            }
        }
    }
}
