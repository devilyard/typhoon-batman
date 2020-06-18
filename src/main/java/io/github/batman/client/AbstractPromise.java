/*
 * Copyright (c) 2019. All rights reserved.
 * AbstractPromise.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author C.
 */
public abstract class AbstractPromise<R, E extends Throwable> implements Promise<R> {

    private E cause;
    private R result;
    private final AtomicReference<Byte> complete = new AtomicReference<>((byte) 0);
    private PromiseListener promiseListener;
    private final ExecutorService executorService;
    private short waiters = 0;

    public AbstractPromise(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setCause(E cause) {
        if (complete.compareAndSet((byte) 0, (byte) 1)) {
            this.cause = cause;
            synchronized (this) {
                complete.set((byte) 2);
                checkNotifyWaiters();
                if (promiseListener != null) {
                    fireActionListener(false);
                }
            }
        } else {
            throw new IllegalStateException("Promise is complete already.");
        }
    }

    public void setResult(R result) {
        if (complete.compareAndSet((byte) 0, (byte) 1)) {
            this.result = result;
            synchronized (this) {
                complete.set((byte) 3);
                checkNotifyWaiters();
                if (promiseListener != null) {
                    fireActionListener(true);
                }
            }
        } else {
            throw new IllegalStateException("Promise is complete already.");
        }
    }

    private void checkNotifyWaiters() {
        synchronized (complete) {
            if (waiters > 0) {
                complete.notifyAll();
            }
        }
    }

    @Override
    public Promise<R> setPromiseListener(PromiseListener listener) {
        synchronized (this) {
            this.promiseListener = listener;
            if (isComplete()) {
                fireActionListener(complete.get() == 3);
            }
        }
        return this;
    }

    @Override
    public PromiseListener getPromiseListener() {
        return promiseListener;
    }

    @Override
    public boolean isComplete() {
        return complete.get() > 1;
    }

    @Override
    public E cause() {
        return cause;
    }

    @Override
    public R getResult() {
        return result;
    }

    @Override
    public Promise<R> waitForCompletion() throws InterruptedException {
        if (isComplete()) {
            return this;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        synchronized (complete) {
            while (!isComplete()) {
                incWaiters();
                try {
                    complete.wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    @Override
    public boolean waitForCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (timeout <= 0) {
            return isComplete();
        }

        if (isComplete()) {
            return true;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        long timeoutNanos = timeUnit.toNanos(timeout);
        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        for (; ; ) {
            synchronized (complete) {
                if (isComplete()) {
                    return true;
                }
                incWaiters();
                try {
                    complete.wait(waitTime / 1000000, (int) (waitTime % 1000000));
                } finally {
                    decWaiters();
                }
            }
            if (isComplete()) {
                return true;
            } else {
                waitTime = timeoutNanos - (System.nanoTime() - startTime);
                if (waitTime <= 0) {
                    return isComplete();
                }
            }
        }
    }

    private void fireActionListener(boolean success) {
        if (success) {
            executorService.execute(() -> promiseListener.onSuccess(this));
        } else {
            executorService.execute(() -> promiseListener.onFailure(this));
        }
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
