/*
 * Copyright (c) 2019. All rights reserved.
 * PublishPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.MqttQoS;
import io.github.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public interface PublishPromise<Message extends MqttMessage> extends ActionPromise<Message> {

    /**
     * Returns the topic name of the PUBLISH message.
     *
     * @return topic name
     */
    String getTopicName();

    /**
     * Returns the mqtt qos level of the PUBLISH message.
     *
     * @return qos
     */
    MqttQoS getQosLevel();

    /**
     * Returns the packetId of the PUBLISH message.
     * <p>For qos 0, -1 will be returned.</p>
     *
     * @return packetId
     */
    int getPacketId();

    /**
     * Returns the payload of the PUBLISH message.
     *
     * @return payload
     */
    byte[] getPayload();
}
