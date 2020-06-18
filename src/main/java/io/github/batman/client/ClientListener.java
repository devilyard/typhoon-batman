/*
 * Copyright (c) 2019. All rights reserved.
 * ClientListener.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttPublishMessage;

/**
 * @author C.
 */
public interface ClientListener {

    /**
     * Fires when the client is connected fist time.
     */
    default void onConnected() {};

    /**
     * Fires when message income.
     *
     * @param message
     */
    default void onMessageReceived(MqttMessage message) {};

    /**
     * Fires when message is sent.
     *
     * @param message
     */
    default void onMessageSent(MqttMessage message) {};

    /**
     * Fires when message is published. For qos0 fires when message is sent, for qos1 fires when PUBACK message
     * is received, and for qos2 fires when PUBCOMP message is received.
     *
     * @param message
     */
    default void onPublished(MqttPublishMessage message) {};

    /**
     * Fires when the client is disconnected. Disconnection may happens because of the server problem and
     * also the client self.
     *
     * @param cause the cause of the disconnection.
     */
    default void onDisconnected(MqttException cause) {};

    /**
     * Fires when the client reconnected to the server.
     */
    default void onReconnected() {};
}
