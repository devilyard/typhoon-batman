/*
 * Copyright (c) 2019. All rights reserved.
 * Tester.java created at 2019-10-08 12:01:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.test;

import org.junit.Test;
import org.typhoon.batman.client.MqttClient;
import org.typhoon.batman.client.MqttException;
import org.typhoon.batman.client.network.MqttConnectOptions;

import java.util.concurrent.CountDownLatch;

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

        new CountDownLatch(1).await();
    }
}
