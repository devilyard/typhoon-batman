/*
 * Copyright (c) 2019. All rights reserved.
 * ScheduledHeartbeatSprite.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.typhoon.batman.client.handler.outbound.MqttPingReqMessageHandler;
import org.typhoon.batman.client.handler.outbound.OutboundMessageHandlerFactory;
import org.typhoon.batman.client.message.MqttMessage;
import org.typhoon.batman.client.message.MqttMessageType;
import org.typhoon.batman.client.message.MqttPingReqMessage;

import java.util.concurrent.*;

/**
 * @author C.
 */
public class ScheduledHeartbeatSprite implements HeartbeatSprite {

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private String clientId;
    private final byte[] pingOutstandingLock = new byte[0];
    private long lastOutboundActivity = 0;
    private long lastInboundActivity = 0;
    private final MqttPingReqMessage pingReqMessage;
    private long keepAliveMillis;
    private int outstandingCount;
    private final ClientContext clientContext;

    public ScheduledHeartbeatSprite(ClientContext context) {
        pingReqMessage = new MqttPingReqMessage();
        this.clientContext = context;
    }

    @Override
    public void init(String clientId, long keepAliveMillis) {
        this.clientId = clientId;
        this.keepAliveMillis = keepAliveMillis;
    }

    @Override
    public void start() {
        if (executorService == null || executorService.isShutdown()) {
            ThreadFactory threadFactory = r -> {
                Thread t = new Thread(r, "mqtt-heartbeat");
                if (!t.isDaemon()) {
                    t.setDaemon(true);
                }
                return t;
            };
            this.executorService = new ScheduledThreadPoolExecutor(1, threadFactory);
        }
        schedule(keepAliveMillis);
    }

    @Override
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    @Override
    public void schedule(long delayInMillis) {
        scheduledFuture = executorService.schedule(new PingTask(), delayInMillis, TimeUnit.MILLISECONDS);
    }

    public void notifySent(MqttMessage message) {
        lastOutboundActivity = System.currentTimeMillis();
    }

    public void notifyReceived(MqttMessage message) {
        lastInboundActivity = System.currentTimeMillis();
        if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            synchronized (pingOutstandingLock) {
                outstandingCount = Math.max(0, outstandingCount - 1);
            }
        }
    }

    private class PingTask implements Runnable {

        public void run() {
            String originalThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName("MQTT Ping: " + clientId);
            try {
                checkForActivity();
            } catch (MqttException e) {
                clientContext.getMessageTransporter().shutdown(e);
            }
            Thread.currentThread().setName(originalThreadName);
        }

        private void checkForActivity() throws MqttException {
            if (clientContext.getClientSession().isQuiescent()) {
                return;
            }
            if (keepAliveMillis > 0) {
                long time = System.currentTimeMillis();
                // @@ Reduce schedule frequency since System.currentTimeMillis is not accurate, add a buffer of 1/10 in minimum keep alive unit.
                int delta = 100;
                long nextPingTime;
                synchronized (pingOutstandingLock) {
                    // @@ The connection may lost because the broker did not reply to my ping in time.
                    if (outstandingCount > 0 && (time - lastInboundActivity >= keepAliveMillis + delta)) {
                        throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
                    }

                    // @@ The connection may lost because I could not get any successful writing for 2 keepAlive intervals?
                    if (outstandingCount == 0 && (time - lastOutboundActivity >= 2 * keepAliveMillis)) {
                        throw new MqttException(MqttException.REASON_CODE_WRITE_TIMEOUT);
                    }

                    // 1. Is a ping required by the client to verify whether the broker is down?
                    //    Condition: ((pingOutstanding == 0 && (time - lastInboundActivity >= keepAlive + delta)))
                    //    In this case only one ping is sent. If not confirmed, client will assume a lost connection to the broker.
                    // 2. Is a ping required by the broker to keep the client alive?
                    //    Condition: (time - lastOutboundActivity >= keepAlive - delta)
                    //    In this case more than one ping outstanding may be necessary.
                    //    This would be the case when receiving a large message;
                    //    the broker needs to keep receiving a regular ping even if the ping response are queued after the long message
                    //    If lacking to do so, the broker will consider my connection lost and cut my socket.
                    if ((outstandingCount == 0 && (time - lastInboundActivity >= keepAliveMillis - delta)) || (time - lastOutboundActivity >= keepAliveMillis - delta)) {
                        nextPingTime = keepAliveMillis;
                        MqttPingReqMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.PINGREQ);
                        handler.handle(pingReqMessage, clientContext);
                        outstandingCount++;
                    } else {
                        nextPingTime = Math.max(1, keepAliveMillis - (time - lastOutboundActivity));
                    }
                }
                schedule(nextPingTime);
            }
        }
    }
}
