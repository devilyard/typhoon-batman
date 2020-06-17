/*
 * Copyright (c) 2019. All rights reserved.
 * ConnectPromise.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.outbound;

import org.typhoon.batman.client.message.MqttConnAckMessage;
import org.typhoon.batman.client.message.MqttVersion;

/**
 * @author C.
 */
public interface ConnectPromise extends ActionPromise<MqttConnAckMessage> {

    /**
     * Returns the username of the CONNECT message.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Returns the password of the CONNECT message.
     *
     * @return the password
     */
    byte[] getPassword();

    /**
     * Returns the clientId of the CONNECT message.
     *
     * @return the clientId
     */
    String getClientId();

    /**
     * Returns the mqtt protocol version which the CONNECT message sent with.
     *
     * @return the mqtt version
     */
    MqttVersion getMqttVersion();

}
