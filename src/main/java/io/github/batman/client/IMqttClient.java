/*
 * Copyright (c) 2019. All rights reserved.
 * IMqttClient.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.network.MqttConnectOptions;
import io.github.batman.MqttQoS;

import java.util.concurrent.TimeUnit;

/**
 * @author C.
 */
public interface IMqttClient {

    /**
     * Connects to server with default connect options.
     * @see {@link MqttConnectOptions}
     *
     * @throws MqttException
     */
    void connect() throws MqttException;

    /**
     * Connects to server with given connect options.
     *
     * @param options connect options for the connect action
     * @throws MqttException
     */
    void connect(MqttConnectOptions options) throws MqttException;

    /**
     * Connects to server with given connect options and offline options.
     *
     * @param connectOptions
     * @param offlineOptions
     * @throws MqttException
     */
    void connect(MqttConnectOptions connectOptions, OfflineOptions offlineOptions) throws MqttException;

    /**
     * Disconnects from the server.
     *
     * @throws MqttException
     */
    void disconnect() throws MqttException;

    /**
     * Disconnects from the server after given {@code timeout}.
     *
     * @param timeout total time to wait before disconnecting.
     * @param timeUnit time unit for {@code timeout} argument
     * @throws MqttException
     */
    void disconnect(long timeout, TimeUnit timeUnit) throws MqttException;

    /**
     * Returns the id of the client.
     *
     * @return clientId the client connects with.
     */
    String getClientId();

    /**
     *
     * @param timeToWait
     * @param timeUnit
     */
    void setTimeToWait(long timeToWait, TimeUnit timeUnit);

    /**
     * Sets the listener for the client.
     *
     * @param listener
     */
    void setClientListener(ClientListener listener);

    /**
     * Publishes a message with given {@code qos} and {@code retain} flag.
     * <p>{@code topicName} should not contain any wildcard("+", "#")</p>
     *
     * @param topicName topicName of the message will be published to
     * @param payload content of the message
     * @param qos qos of the message, see {@link MqttQoS}
     * @param retained retain flag for the message
     * @throws MqttException
     */
    void publish(String topicName, byte[] payload, MqttQoS qos, boolean retained) throws MqttException;

    /**
     * Publishes a message with default qos(qos = 0) and retain flag(0).
     * <p>{@code topicName} should not contain any wildcard("+", "#")</p>
     *
     * @param topicName topicName of the message will be published to
     * @param payload content of the message
     * @throws MqttException
     */
    void publish(String topicName, byte[] payload) throws MqttException;

    /**
     * Subscribes a topic with default qos(2).
     *
     * @param topicFilter topicFilter to subscribe to
     * @param listener message listener of the subscription
     * @throws MqttException
     */
    void subscribe(String topicFilter, MessageListener listener) throws MqttException;

    /**
     * Subscribes a topic with given {@code topicFilter}, {@code topicFilter} may contain wildcard.
     *
     * @param topicFilter topicFilter to subscribe to
     * @param qos qos of the subscription
     * @param listener message listener of the subscription
     * @throws MqttException
     */
    void subscribe(String topicFilter, MqttQoS qos, MessageListener listener) throws MqttException;

    /**
     * Subscribes multiple topics with one message listener. the {@code topicFilters} and {@code qoses}
     * must have the same length.
     *
     * @param topicFilters topicFilters to subscribe to
     * @param qoses qoses for the subscription
     * @param listener message listener of the subscription
     * @throws MqttException
     */
    void subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener listener) throws MqttException;

    /**
     * Subscribes multiple topics with multiple listeners. {@code topicFilters}, {@code qoses} and {@code listeners}
     * must have the same length.
     *
     * @param topicFilters topicFilters to subscribe to
     * @param qoses qoses for the subscription
     * @param listeners message listeners of the subscription
     * @throws MqttException
     */
    void subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener[] listeners) throws MqttException;

    /**
     * Unsubscribes a topic.
     *
     * @param topicFilter topicFilter to unsubscribe
     * @throws MqttException
     */
    void unsubscribe(String topicFilter) throws MqttException;

    /**
     * Unsubscribes multiple topics.
     *
     * @param topicFilters
     * @throws MqttException
     */
    void unsubscribe(String[] topicFilters) throws MqttException;

    /**
     * Closes the client. If the client is closed, it cannot be connect again.
     *
     * @throws MqttException
     */
    void close() throws MqttException;

    /**
     * Returns if the client is connected.
     *
     * @return
     */
    boolean isConnected();
}
