/*
 * Copyright (c) 2019. All rights reserved.
 * AbstractActionPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.AbstractPromise;
import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.PromiseListener;
import org.typhoon.batman.client.message.MqttMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author C.
 */
public abstract class AbstractActionPromise<Message extends MqttMessage> extends AbstractPromise<Message, MqttException> implements ActionPromise<Message> {

    private short waiters = 0;
    private final AtomicBoolean sent = new AtomicBoolean(false);

    public AbstractActionPromise(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public MqttException cause() {
        return super.cause();
    }

    @Override
    public ActionPromise<Message> waitForCompletion() throws InterruptedException {
        return (ActionPromise<Message>) super.waitForCompletion();
    }

    @Override
    public ActionPromise<Message> setPromiseListener(PromiseListener listener) {
        return (ActionPromise<Message>) super.setPromiseListener(listener);
    }

    @Override
    public ActionPromise<Message> waitUntilSent() throws InterruptedException {
        if (isComplete() || sent.get()) {
            return this;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        synchronized (sent) {
            while (!sent.get()) {
                incWaiters();
                try {
                    sent.wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    public void notifySent() {
        if (sent.compareAndSet(false, true)) {
            synchronized (sent) {
                if (waiters > 0) {
                    sent.notifyAll();
                }
            }
        }
    }

    public void notifySendFailed(MqttException cause) {
        notifySent();
        setCause(cause);
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters: " + this);
        }
        ++waiters;
    }

    private void decWaiters() {
        --waiters;
    }
}
