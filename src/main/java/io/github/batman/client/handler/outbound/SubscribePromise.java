/*
 * Copyright (c) 2019. All rights reserved.
 * SubscribePromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.MqttQoS;
import io.github.batman.client.MessageListener;
import io.github.batman.client.message.MqttSubAckMessage;

import java.util.Map;

/**
 * @author C.
 */
public interface SubscribePromise extends ActionPromise<MqttSubAckMessage> {

    /**
     * Returns the topic filters and qos sent by SUBSCRIBE message.
     *
     * @return topic filter and qos
     */
    Map<String, Map<MqttQoS, MessageListener>> getSubscriptions();
}
