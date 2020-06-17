/*
 * Copyright (c) 2019. All rights reserved.
 * UnsubscribePromiseImpl.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.message.MqttUnsubAckMessage;

import java.util.concurrent.ExecutorService;

/**
 * @author C.
 */
public class UnsubscribePromiseImpl extends AbstractActionPromise<MqttUnsubAckMessage> implements UnsubscribePromise {

    private final String[] topicFilters;

    public UnsubscribePromiseImpl(String[] topicFilters, ExecutorService executorService) {
        super(executorService);
        this.topicFilters = topicFilters;
    }

    @Override
    public String[] getTopicFilters() {
        return topicFilters;
    }
}
