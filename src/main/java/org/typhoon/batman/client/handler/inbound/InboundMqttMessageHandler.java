/*
 * Copyright (c) 2019. All rights reserved.
 * InboundMqttMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.typhoon.batman.client.handler.MqttMessageHandler;
import org.typhoon.batman.client.message.MqttMessage;

/**
 * @author C.
 */
public interface InboundMqttMessageHandler<Message extends MqttMessage> extends MqttMessageHandler<Message> {
}
