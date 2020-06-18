/*
 * Copyright (c) 2019. All rights reserved.
 * Qos2PublishPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.MqttPubCompMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
public class Qos2PublishPromise extends AbstractPublishPromise<MqttPubCompMessage> {

    public Qos2PublishPromise(String topicName, MqttQoS qosLevel, int packetId, byte[] payload, ExecutorService executorService) {
        super(topicName, qosLevel, packetId, payload, executorService);
    }
}
