/*
 * Copyright (c) 2019. All rights reserved.
 * Qos1PublishPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.MqttPubAckMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
public class Qos1PublishPromise extends AbstractPublishPromise<MqttPubAckMessage> {

    public Qos1PublishPromise(String topicName, MqttQoS qosLevel, int packetId, byte[] payload, ExecutorService executorService) {
        super(topicName, qosLevel, packetId, payload, executorService);
    }
}
