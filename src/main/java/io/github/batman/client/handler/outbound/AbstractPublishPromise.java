/*
 * Copyright (c) 2019. All rights reserved.
 * AbstractPublishPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.MqttMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
abstract public class AbstractPublishPromise<Message extends MqttMessage> extends AbstractActionPromise<Message> implements PublishPromise<Message> {

    private final String topicName;
    private final MqttQoS qosLevel;
    private final int packetId;
    private final byte[] payload;

    public AbstractPublishPromise(String topicName, MqttQoS qosLevel, int packetId, byte[] payload, ExecutorService executorService) {
        super(executorService);
        this.topicName = topicName;
        this.qosLevel = qosLevel;
        this.packetId = packetId;
        this.payload = payload;
    }

    @Override
    public String getTopicName() {
        return topicName;
    }

    @Override
    public MqttQoS getQosLevel() {
        return qosLevel;
    }

    @Override
    public int getPacketId() {
        return packetId;
    }

    @Override
    public byte[] getPayload() {
        return payload;
    }
}
