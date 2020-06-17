/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSubscribePayload.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import java.util.Collections;
import java.util.List;

/**
 * @author C.
 */
public final class MqttSubscribePayload {

    private final List<MqttTopicSubscription> topicSubscriptions;

    public MqttSubscribePayload(List<MqttTopicSubscription> topicSubscriptions) {
        this.topicSubscriptions = Collections.unmodifiableList(topicSubscriptions);
    }

    public List<MqttTopicSubscription> topicSubscriptions() {
        return topicSubscriptions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('[');
        for (int i = 0; i < topicSubscriptions.size() - 1; i++) {
            builder.append(topicSubscriptions.get(i)).append(", ");
        }
        builder.append(topicSubscriptions.get(topicSubscriptions.size() - 1));
        builder.append(']');
        return builder.toString();
    }
}