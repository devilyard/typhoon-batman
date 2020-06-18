/*
 * Copyright (c) 2019. All rights reserved.
 * PromiseListener.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

/**
 * @author C.
 */
public interface PromiseListener {

    /**
     * This method is invoked when an action has completed successfully.
     *
     * @param promise associated with the action that has completed
     */
    void onSuccess(Promise<?> promise);

    /**
     * This method is invoked when an action fails.
     * If a client is disconnected while an action is in progressing
     * onFailure will be called. For connections
     * that use cleanSession set to false, any QoS 1 and 2 messages that
     * are in the process of being delivered will be delivered to the requested
     * quality of service next time the client connects.
     *
     * @param promise associated with the action that has failed
     */
    void onFailure(Promise<?> promise);

}
