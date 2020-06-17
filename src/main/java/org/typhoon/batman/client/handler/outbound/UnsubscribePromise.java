/*
 * Copyright (c) 2019. All rights reserved.
 * UnsubscribePromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.message.MqttUnsubAckMessage;

/**
 * @author C.
 */
public interface UnsubscribePromise extends ActionPromise<MqttUnsubAckMessage> {

    /**
     * Returns the topic filters that unsubscribed by UNSUBSCRIBE message.
     *
     * @return unsubscribed topic filters
     */
    String[] getTopicFilters();
}
