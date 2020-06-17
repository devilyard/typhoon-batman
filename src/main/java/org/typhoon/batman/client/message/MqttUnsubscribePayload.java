/*
 * Copyright (c) 2019. All rights reserved.
 * MqttUnsubscribePayload.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import java.util.Collections;
import java.util.List;

/**
 * @author C.
 */
public final class MqttUnsubscribePayload {

    private final List<String> topics;

    public MqttUnsubscribePayload(List<String> topics) {
        this.topics = Collections.unmodifiableList(topics);
    }

    public List<String> topics() {
        return topics;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('[');
        for (int i = 0; i < topics.size() - 1; i++) {
            builder.append("topicName = ").append(topics.get(i)).append(", ");
        }
        builder.append("topicName = ").append(topics.get(topics.size() - 1)).append(']');
        return builder.toString();
    }
}