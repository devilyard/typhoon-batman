/*
 * Copyright (c) 2019. All rights reserved.
 * MqttSecurityException.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */

package org.typhoon.batman.client.network;

import org.typhoon.batman.client.MqttException;

/**
 *
 * @author C.
 */
public class MqttSecurityException extends MqttException {

	/**
	 * Constructs a new <code>MqttSecurityException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttSecurityException(Throwable cause) {
		super(cause);
	}
}
