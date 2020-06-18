/*
 * Copyright (c) 2019. All rights reserved.
 * MqttAsyncClient.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.handler.inbound.*;
import io.github.batman.client.handler.outbound.*;
import io.github.batman.client.network.ConnectionFactory;
import io.github.batman.client.network.MqttConnectOptions;
import io.github.batman.client.store.MemorySessionStore;
import io.github.batman.client.store.SessionStore;
import org.apache.commons.lang3.StringUtils;
import io.github.batman.Constants;
import io.github.batman.MqttQoS;
import org.typhoon.batman.client.handler.inbound.*;
import org.typhoon.batman.client.handler.outbound.*;
import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttMessageType;
import io.github.batman.client.message.MqttVersion;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.batman.Constants.*;

/**
 * @author C.
 */
public class MqttAsyncClient implements IMqttAsyncClient {

    private final String serverURI;
    private SessionStore sessionStore;
    private final MessageTransporter messageTransporter;
    private MqttConnectOptions connectOptions;
    private final ClientSession clientSession;
    private final HeartbeatSprite heartbeatSprite;

    public MqttAsyncClient(String serverURI) {
        this(serverURI, null, null);
    }

    public MqttAsyncClient(String serverURI, SessionStore sessionStore, ScheduledExecutorService executorService) {
        ConnectionFactory.validateURI(serverURI);
        this.serverURI = serverURI;
        if (executorService == null) {
            int size = Runtime.getRuntime().availableProcessors() * 2;
            executorService = new ScheduledThreadPoolExecutor(size);
        }
        this.sessionStore = sessionStore;
        if (sessionStore == null) {
            this.sessionStore = new MemorySessionStore();
        }

        ClientContext context = new ClientContext();
        PromiseKeeper promiseKeeper = new PromiseKeeper();
        heartbeatSprite = new ScheduledHeartbeatSprite(context);
        clientSession = new ClientSession(this.sessionStore, promiseKeeper, heartbeatSprite);
        this.messageTransporter = new MessageTransporter(context, executorService);
        context.setClient(this);
        context.setClientSession(clientSession);
        context.setMessageTransporter(messageTransporter);
        context.setPromiseKeeper(promiseKeeper);
        context.setSessionStore(this.sessionStore);
        context.setMessageNotifier(new SubscribedMessageNotifier());
    }

    @Override
    public ConnectPromise connect() throws MqttException {
        return connect(null, null);
    }

    @Override
    public ConnectPromise connect(MqttConnectOptions options) throws MqttException {
        return connect(options, null);
    }

    @Override
    public ConnectPromise connect(MqttConnectOptions connectOptions, OfflineOptions offlineOptions) throws MqttException {
        if (clientSession.isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED);
        }
        if (clientSession.isConnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
        }
        if (clientSession.isDisconnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
        }
        if (clientSession.isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        if (connectOptions == null) {
            connectOptions = new MqttConnectOptions();
        }
        String clientId = connectOptions.getClientId();
        if (clientId == null) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_CLIENT_ID);
        }
        this.connectOptions = connectOptions;
        if (offlineOptions == null) {
            offlineOptions = new OfflineOptions();
        }
        int clientIdLength = 0;
        for (int i = 0; i < clientId.length() - 1; i++) {
            if (isHighSurrogate(clientId.charAt(i))) {
                i++;
            }
            clientIdLength++;
        }
        MqttVersion version = connectOptions.getMqttVersion();
        if (version == MqttVersion.MQTT_3_1 && (clientIdLength > MAX_CLIENT_ID_LENGTH_V3_1 || clientIdLength < MIN_CLIENT_ID_LENGTH_V3_1)) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_CLIENT_ID);
        }
        if (version == MqttVersion.MQTT_3_1_1 && clientIdLength > MAX_CLIENT_ID_LENGTH_V3_1_1) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_CLIENT_ID);
        }
        clientSession.setClientId(clientId);
        sessionStore.setMaxInFlight(connectOptions.getMaxInflight());
        sessionStore.setOfflineOptions(offlineOptions);
        initMessageHandlers();
        heartbeatSprite.init(clientId, connectOptions.getKeepAliveInterval() * 1000);
        return messageTransporter.connect(serverURI, connectOptions, offlineOptions);
    }

    @Override
    public DisconnectPromise disconnect() throws MqttException {
        return disconnect(30, TimeUnit.SECONDS);
    }

    @Override
    public DisconnectPromise disconnect(long timeout, TimeUnit timeUnit) throws MqttException {
        return messageTransporter.disconnect(timeout, timeUnit);
    }

    @Override
    public void setClientListener(ClientListener listener) {
        messageTransporter.setClientListener(listener);
    }

    @Override
    public void close() throws MqttException {
        messageTransporter.close(null);
    }

    @Override
    public PublishPromise<? extends MqttMessage> publish(String topicName, byte[] payload, MqttQoS qos, boolean retained) throws MqttException {
        validTopic(topicName, false);
        if (payload.length > MAX_PAYLOAD_LENGTH) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_MESSAGE);
        }
        return messageTransporter.publish(topicName, payload, qos, retained);
    }

    @Override
    public PublishPromise<? extends MqttMessage> publish(String topicName, byte[] payload) throws MqttException {
        return publish(topicName, payload, MqttQoS.AT_MOST_ONCE, false);
    }

    @Override
    public SubscribePromise subscribe(String topicFilter, MessageListener listener) throws MqttException {
        return subscribe(topicFilter, MqttQoS.EXACTLY_ONCE, listener);
    }

    @Override
    public SubscribePromise subscribe(String topicFilter, MqttQoS qos, MessageListener listener) throws MqttException {
        validTopic(topicFilter, true);
        return messageTransporter.subscribe(topicFilter, qos, listener);
    }

    @Override
    public SubscribePromise subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener listener) throws MqttException {
        if (topicFilters.length == 0 || qoses.length == 0 || topicFilters.length != qoses.length) {
            throw new IllegalArgumentException("Invalid topic length and qos length");
        }
        for (String topicFilter : topicFilters) {
            validTopic(topicFilter, true);
        }
        return messageTransporter.subscribe(topicFilters, qoses, listener);
    }

    @Override
    public SubscribePromise subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener[] listeners) throws MqttException {
        if (topicFilters.length == 0 || qoses.length == 0 || listeners.length == 0 || topicFilters.length != qoses.length || qoses.length != listeners.length) {
            throw new IllegalArgumentException("Invalid topic length, qos length or listener length");
        }
        for (String topicFilter : topicFilters) {
            validTopic(topicFilter, true);
        }
        return messageTransporter.subscribe(topicFilters, qoses, listeners);
    }

    @Override
    public UnsubscribePromise unsubscribe(String topicFilter) throws MqttException {
        validTopic(topicFilter, true);
        return messageTransporter.unsubscribe(topicFilter);
    }

    @Override
    public UnsubscribePromise unsubscribe(String[] topicFilters) throws MqttException {
        if (topicFilters.length == 0) {
            throw new IllegalArgumentException("Empty topicFilters.");
        }
        for (String topicFilter : topicFilters) {
            validTopic(topicFilter, true);
        }
        return messageTransporter.unsubscribe(topicFilters);
    }

    /**
     *
     * @param topic
     * @param allowWildcard
     * @throws MqttException
     */
    private void validTopic(String topic, boolean allowWildcard) throws MqttException {
        MqttVersion version = connectOptions.getMqttVersion() == null ? MqttVersion.MQTT_3_1_1 : connectOptions.getMqttVersion();
        if (StringUtils.isEmpty(topic) ||
                (version == MqttVersion.MQTT_3_1 && topic.getBytes(StandardCharsets.UTF_8).length > MAX_TOPIC_LENGTH_V3_1) ||
                (version == MqttVersion.MQTT_3_1_1 && topic.getBytes(StandardCharsets.UTF_8).length > MAX_TOPIC_LENGTH_V_3_1_1)) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_TOPIC);
        }
        if (!allowWildcard) {
            for (char c : Constants.TOPIC_WILDCARDS) {
                if (topic.indexOf(c) >= 0) {
                    throw new MqttException(MqttException.REASON_CODE_INVALID_TOPIC);
                }
            }
        }
        if (!Topic.asTopic(topic).isValid()) {
            throw new MqttException(MqttException.REASON_CODE_INVALID_TOPIC);
        }
    }

    @Override
    public String getClientId() {
        return connectOptions.getClientId();
    }

    @Override
    public boolean isConnected() {
        return clientSession.isConnected();
    }


    private boolean isHighSurrogate(char ch) {
        return (ch >= Character.MIN_HIGH_SURROGATE) && (ch <= Character.MAX_HIGH_SURROGATE);
    }

    private void initMessageHandlers() {
        // @@ outbound
        MqttConnectMessageHandler connectMessageHandler = new MqttConnectMessageHandler();
        OutboundMessageHandlerFactory.register(MqttMessageType.CONNECT, connectMessageHandler);

        MqttPingReqMessageHandler pingReqMessageHandler = new MqttPingReqMessageHandler();
        OutboundMessageHandlerFactory.register(MqttMessageType.PINGREQ, pingReqMessageHandler);

        MqttOutboundPublishMessageHandler outboundPublishMessageHandler = new MqttOutboundPublishMessageHandler();
        OutboundMessageHandlerFactory.register(MqttMessageType.PUBLISH, outboundPublishMessageHandler);

        MqttSubscribeMessageHandler subscribeMessageHandler = new MqttSubscribeMessageHandler();
        OutboundMessageHandlerFactory.register(MqttMessageType.SUBSCRIBE, subscribeMessageHandler);

        MqttUnsubscribeMessageHandler unsubscribeMessageHandler = new MqttUnsubscribeMessageHandler();
        OutboundMessageHandlerFactory.register(MqttMessageType.UNSUBSCRIBE, unsubscribeMessageHandler);

        // @@ inbound
        MqttConnAckMessageHandler connAckMessageHandler = new MqttConnAckMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.CONNACK, connAckMessageHandler);

        MqttPingRespMessageHandler pingRespMessageHandler = new MqttPingRespMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PINGRESP, pingRespMessageHandler);

        MqttPubAckMessageHandler pubAckMessageHandler = new MqttPubAckMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PUBACK, pubAckMessageHandler);

        MqttPubRecMessageHandler pubRecMessageHandler = new MqttPubRecMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PUBREC, pubRecMessageHandler);

        MqttPubCompMessageHandler pubCompMessageHandler = new MqttPubCompMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PUBCOMP, pubCompMessageHandler);

        MqttSubAckMessageHandler subAckMessageHandler = new MqttSubAckMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.SUBACK, subAckMessageHandler);

        MqttInboundPublishMessageHandler inboundPublishMessageHandler = new MqttInboundPublishMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PUBLISH, inboundPublishMessageHandler);

        MqttPubRelMessageHandler pubRelMessageHandler = new MqttPubRelMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.PUBREL, pubRelMessageHandler);

        MqttUnsubAckMessageHandler unsubAckMessageHandler = new MqttUnsubAckMessageHandler();
        InboundMessageHandlerFactory.register(MqttMessageType.UNSUBACK, unsubAckMessageHandler);
    }
}
