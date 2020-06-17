/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnAckMessageHandler.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.handler.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.client.ClientContext;
import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.handler.outbound.ConnectPromiseImpl;
import org.typhoon.batman.client.message.MqttConnAckMessage;
import org.typhoon.batman.client.message.MqttConnectReturnCode;
import org.typhoon.batman.client.message.MqttVersion;
import org.typhoon.batman.client.network.MqttConnectOptions;

/**
 * @author C.
 */
public class MqttConnAckMessageHandler implements InboundMqttMessageHandler<MqttConnAckMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnAckMessageHandler.class);

    @Override
    public void handle(MqttConnAckMessage message, ClientContext context) {
        MqttConnectReturnCode returnCode = message.variableHeader().connectReturnCode();
        LOGGER.info("Connection to server respond with code: {}, clientId: {}", returnCode, context.getClientSession().getClientId());
        ConnectPromiseImpl promise = (ConnectPromiseImpl) context.getPromiseKeeper().remove(message);
        if (returnCode == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
            promise.setResult(message);
            context.getMessageTransporter().notifyConnected();
        } else if (returnCode == MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION) {
            MqttConnectOptions connectOptions = context.getMessageTransporter().getConnectOptions();
            if (connectOptions.getMqttVersion() == null) {
                context.getMessageTransporter().getConnectOptions().setMqttVersion(MqttVersion.MQTT_3_1);
                try {
                    context.getClient().connect();
                } catch (MqttException e) {
                    promise.setCause(e);
                }
            } else {
                promise.setCause(new MqttException(MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION));
            }
        } else {
            promise.setCause(new MqttException(returnCode.value()));
        }
    }
}
