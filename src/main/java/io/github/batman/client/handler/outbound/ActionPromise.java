/*
 * Copyright (c) 2019. All rights reserved.
 * ActionPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.handler.outbound;

import io.github.batman.client.MqttException;
import io.github.batman.client.Promise;
import io.github.batman.client.message.MqttMessage;

import java.util.concurrent.TimeUnit;

/**
 * @author C.
 */
public interface ActionPromise<Message extends MqttMessage> extends Promise<Message> {

    /**
     * Blocks the current thread until the operation this promise is associated with has completed.
     *
     * @return the promise self
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #waitForCompletion(long, TimeUnit)
     */
    ActionPromise<Message> waitForCompletion() throws InterruptedException;

    /**
     * Blocks the current thread until message is sent.
     *
     * @return the promise self
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    ActionPromise<Message> waitUntilSent() throws InterruptedException;

    /**
     * Returns an exception providing more detail if an operation failed.
     * <p>While the operation is in progress or completes successfully
     * null will be returned. Certain errors like timeout or shutting down will not
     * set the exception as the operation has not failed or completed at that time
     * </p>
     *
     * @return exception may return an exception if the operation failed. Null will be
     * returned while operation is in progress or completes successfully.
     */
    MqttException cause();
}
