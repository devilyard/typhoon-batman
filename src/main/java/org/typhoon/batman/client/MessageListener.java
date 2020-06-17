/*
 * Copyright (c) 2019. All rights reserved.
 * MessageListener.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.typhoon.batman.client.message.MqttPublishMessage;

/**
 * A message listener for received publish messages the client subscribed.
 *
 * @author C.
 */
public interface MessageListener {

    /**
     * Fires when publish message is received.
     *
     * @param message
     */
    void onMessage(MqttPublishMessage message);
}
