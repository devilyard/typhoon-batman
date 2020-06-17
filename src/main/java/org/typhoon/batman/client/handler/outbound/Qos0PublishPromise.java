/*
 * Copyright (c) 2019. All rights reserved.
 * Qos0PublishPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.MqttQoS;
import org.typhoon.batman.client.message.MqttMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
public class Qos0PublishPromise extends AbstractPublishPromise<MqttMessage> {

    public Qos0PublishPromise(String topicName, MqttQoS qosLevel, byte[] payload, ExecutorService executorService) {
        super(topicName, qosLevel, -1, payload, executorService);
    }
}
