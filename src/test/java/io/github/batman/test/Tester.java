/*
 * Copyright (c) 2019. All rights reserved.
 * Tester.java created at 2019-10-08 12:01:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.test;

import io.github.batman.client.MqttClient;
import io.github.batman.client.MqttException;
import io.github.batman.client.network.MqttConnectOptions;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author C.
 */
public class Tester {

    @Test
    public void test1() throws MqttException, InterruptedException {
        MqttClient client = new MqttClient("tcp://127.0.0.1:1088");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUsername("app");
        options.setPassword("passwd".getBytes());
        options.setClientId("test-app");
        options.setAutomaticReconnect(true);
        client.connect(options);

        new CountDownLatch(1).await(2, TimeUnit.SECONDS);
    }
}
