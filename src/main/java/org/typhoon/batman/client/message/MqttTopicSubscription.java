/*
 * Copyright (c) 2019. All rights reserved.
 * MqttTopicSubscription.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * @author C.
 */
public final class MqttTopicSubscription {

    private final String topicFilter;
    private final MqttQoS qualityOfService;

    public MqttTopicSubscription(String topicFilter, MqttQoS qualityOfService) {
        this.topicFilter = topicFilter;
        this.qualityOfService = qualityOfService;
    }

    public String topicName() {
        return topicFilter;
    }

    public MqttQoS qualityOfService() {
        return qualityOfService;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[topicFilter=" + topicFilter + ", qualityOfService=" + qualityOfService + ']';
    }
}