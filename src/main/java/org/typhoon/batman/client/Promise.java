/*
 * Copyright (c) 2019. All rights reserved.
 * Promise.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import java.util.concurrent.TimeUnit;

/**
 * @author C.
 */
public interface Promise<R> {

    /**
     * Blocks current thread until the operation has completed.
     *
     * @return the promise self
     * @throws InterruptedException if current thread is interrupted while waiting
     * @see #waitForCompletion(long, TimeUnit)
     */
    Promise<R> waitForCompletion() throws InterruptedException;

    /**
     * Blocks current thread until the operation has completed.
     * <p>The value of timeout specifies the maximum time it will block for. If the operation
     * completes before timeout then the method returns immediately, else it will block until
     * timeout. </p>
     * <p>If the action being tracked fails or timeout an exception will
     * be thrown. Keep in mind, the action may complete after timeout.
     * </p>
     *
     * @param timeout  the maximum amount of time to wait for
     * @param timeUnit the time unit of the {@code timeout} argument
     * @throws InterruptedException if current thread is interrupted while waiting
     */
    boolean waitForCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * Returns whether or not the action has finished.
     * <p>True will be returned both in the case the action finished successfully
     * and in the case it failed. If the action failed {@link #cause()} will
     * be non null.
     * </p>
     *
     * @return whether or not the action has finished.
     */
    boolean isComplete();

    /**
     * Returns an exception providing more detail if an operation failed.
     * <p>While the operation is in progress or completes successfully
     * null will be returned. Certain errors like timeout or shutting down will not
     * get any exception as the operation has not failed or completed at that time
     * </p>
     *
     * @return may return an exception if the operation failed. Null will be
     * returned while operation is in progress or completes successfully.
     */
    Throwable cause();

    /**
     * Register a listener to be notified when the operation completes.
     * <p>Once a listener is registered it will be invoked when the operation either succeeds or fails.
     * </p>
     *
     * @param listener to be invoked once the action completes
     */
    Promise<R> setPromiseListener(PromiseListener listener);

    /**
     * Return the async listener of this promise.
     *
     * @return listener that is set on the promise or null if not set.
     */
    PromiseListener getPromiseListener();

    /**
     * Returns result of the operation, null will be returned while operation is in progress or is failed.
     *
     * @return result of the operation
     */
    R getResult();
}
