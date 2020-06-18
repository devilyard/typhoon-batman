/*
 * Copyright (c) 2019. All rights reserved.
 * HeartbeatSprite.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public interface HeartbeatSprite {

    void init(String clientId, long keepAliveMillis);

    void start();

    void stop();

    void schedule(long delayInMillis);

    void notifySent(MqttMessage message);

    void notifyReceived(MqttMessage message);
}
