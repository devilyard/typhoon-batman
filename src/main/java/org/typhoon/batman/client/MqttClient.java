/*
 * Copyright (c) 2019. All rights reserved.
 * MqttClient.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.typhoon.batman.MqttQoS;
import org.typhoon.batman.client.handler.outbound.ActionPromise;
import org.typhoon.batman.client.network.MqttConnectOptions;
import org.typhoon.batman.client.store.SessionStore;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author C.
 */
public class MqttClient implements IMqttClient {

    private long timeToWait;
    private TimeUnit timeUnit;
    private final IMqttAsyncClient client;

    public MqttClient(String serverURI) {
        client = new MqttAsyncClient(serverURI);
    }

    public MqttClient(String serverURI, SessionStore sessionStore, ScheduledExecutorService executorService) {
        client = new MqttAsyncClient(serverURI, sessionStore, executorService);
    }

    @Override
    public void connect() throws MqttException {
        waitForCompletion(client.connect());
    }

    @Override
    public void connect(MqttConnectOptions options) throws MqttException {
        waitForCompletion(client.connect(options));
    }

    @Override
    public void connect(MqttConnectOptions connectOptions, OfflineOptions offlineOptions) throws MqttException {
        waitForCompletion(client.connect(connectOptions, offlineOptions));
    }

    @Override
    public void disconnect() throws MqttException {
        waitForCompletion(client.disconnect());
    }

    @Override
    public void disconnect(long timeout, TimeUnit timeUnit) throws MqttException {
        waitForCompletion(client.disconnect(timeout, timeUnit));
    }

    @Override
    public String getClientId() {
        return client.getClientId();
    }

    @Override
    public void setClientListener(ClientListener listener) {
        client.setClientListener(listener);
    }

    @Override
    public void publish(String topicName, byte[] payload, MqttQoS qos, boolean retained) throws MqttException {
        waitForCompletion(client.publish(topicName, payload, qos, retained));
    }

    @Override
    public void publish(String topicName, byte[] payload) throws MqttException {
        waitForCompletion(client.publish(topicName, payload));
    }

    @Override
    public void subscribe(String topicFilter, MessageListener listener) throws MqttException {
        waitForCompletion(client.subscribe(topicFilter, listener));
    }

    @Override
    public void subscribe(String topicFilter, MqttQoS qos, MessageListener listener) throws MqttException {
        waitForCompletion(client.subscribe(topicFilter, qos, listener));
    }

    @Override
    public void subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener listener) throws MqttException {
        waitForCompletion(client.subscribe(topicFilters, qoses, listener));
    }

    @Override
    public void subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener[] listeners) throws MqttException {
        waitForCompletion(client.subscribe(topicFilters, qoses, listeners));
    }

    @Override
    public void unsubscribe(String topicFilter) throws MqttException {
        waitForCompletion(client.unsubscribe(topicFilter));
    }

    @Override
    public void unsubscribe(String[] topicFilters) throws MqttException {
        waitForCompletion(client.unsubscribe(topicFilters));
    }

    @Override
    public void close() throws MqttException {
        client.close();
    }

    @Override
    public void setTimeToWait(long time, TimeUnit timeUnit) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        this.timeToWait = time;
        this.timeUnit = timeUnit;
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    private void waitForCompletion(ActionPromise<?> promise) throws MqttException {
        try {
            if (timeToWait == 0) {
                promise.waitForCompletion();
            } else {
                promise.waitForCompletion(timeToWait, timeUnit);
            }
        } catch (InterruptedException ignore) {
        }
        if (promise.cause() != null) {
            throw promise.cause();
        }
        if (!promise.isComplete()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
        }
    }
}
