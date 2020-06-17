/*
 * Copyright (c) 2019. All rights reserved.
 * MessageTransporter.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typhoon.batman.MqttQoS;
import org.typhoon.batman.client.handler.inbound.InboundMessageHandlerFactory;
import org.typhoon.batman.client.handler.inbound.InboundMqttMessageHandler;
import org.typhoon.batman.client.handler.outbound.*;
import org.typhoon.batman.client.message.*;
import org.typhoon.batman.client.network.*;
import org.typhoon.batman.client.store.SessionStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author C.
 */
public class MessageTransporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTransporter.class);

    private OfflineMessageSender offlineMessageSender;
    private MqttConnectOptions connectOptions;
    private OfflineOptions offlineOptions;
    private Receiver receiver;
    private Sender sender;
    private IConnection connection;
    private boolean closePending;
    private final ClientContext clientContext;
    private ClientListener clientListener;
    private String serverURI;
    private Timer reconnectTimer;
    private long reconnectDelay = 1000;
    /**
     * this thread pool executor is for message processing and event handling.
     */
    private final ExecutorService executorService;
    /**
     * worker thread pool is for internal workers.
     */
    private ThreadPoolExecutor workerThreadPoolExecutor;

    public MessageTransporter(ClientContext context, ExecutorService executorService) {
        this.clientContext = context;
        this.executorService = executorService;
    }

    /**
     * @param clientListener
     */
    public void setClientListener(ClientListener clientListener) {
        this.clientListener = clientListener;
    }

    /**
     * @param address
     * @param options
     * @return
     */
    public ConnectPromise connect(String address, MqttConnectOptions options, OfflineOptions offlineOptions) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        if (clientContext.getClientSession().isConnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
        }
        if (clientContext.getClientSession().isDisconnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
        }

        initWorkerThreadPool();

        ClientSession clientSession = clientContext.getClientSession();
        SessionStore sessionStore = clientContext.getSessionStore();

        clientContext.getClientSession().aquireStateLock();
        try {
            if (clientSession.isDisconnected()) {
                this.serverURI = address;
                this.connectOptions = options;
                this.offlineOptions = offlineOptions;
                clientSession.updateState(ConnectionState.CONNECTING);
                try {
                    sessionStore.open(address, options.getClientId());
                } catch (MqttException e) {
                    clientSession.updateState(ConnectionState.DISCONNECTED);
                    throw e;
                }
                MqttVersion version = options.getMqttVersion() == null ? MqttVersion.MQTT_3_1_1 : options.getMqttVersion();
                MqttPublishMessage willMessage = options.getWillMessage();
                MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                        version.protocolName(),
                        version.protocolLevel(),
                        options.getUsername() != null,
                        options.getPassword() != null,
                        willMessage != null && willMessage.fixedHeader().isRetain(),
                        willMessage != null ? willMessage.fixedHeader().qosLevel().value() : 0,
                        willMessage != null,
                        options.isCleanSession(),
                        options.getKeepAliveInterval()
                );
                MqttConnectPayload payload = new MqttConnectPayload(
                        options.getClientId(),
                        willMessage != null ? willMessage.variableHeader().topicName() : null,
                        willMessage != null ? willMessage.payload() : null,
                        options.getUsername(),
                        options.getPassword()
                );
                MqttConnectMessage connectMessage = new MqttConnectMessage(variableHeader, payload);
                ConnectPromise promise = new ConnectPromiseImpl(options.getUsername(), options.getPassword(), options.getClientId(), version, executorService);
                clientContext.getPromiseKeeper().keep(connectMessage, promise);
                Connector connector = new Connector(address, options, workerThreadPoolExecutor);
                connector.start(connectMessage);
                return promise;
            } else {
                if (clientSession.isConnecting()) {
                    throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
                }
                if (clientSession.isDisconnecting()) {
                    throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
                }
                throw new MqttException(MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED);
            }
        } finally {
            clientSession.releaseStateLock();
        }
    }

    /**
     * init internal worker thread pool. Internal workers such as sender, receiver is keep running all the time,
     * executor set by outside does not surely have enough size, so we need to manage worker thread pool by self.
     */
    private void initWorkerThreadPool() {
        ThreadFactory threadFactory = new ThreadFactory() {

            private final AtomicLong threadNumber = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                String namePrefix = "pool-worker-thread-";
                Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                if (!t.isDaemon()) {
                    t.setDaemon(true);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                t.setUncaughtExceptionHandler((t1, e) -> LOGGER.error("Error occurred in thread: {}", t1.getName(), e));
                return t;
            }
        };
        this.workerThreadPoolExecutor = new ThreadPoolExecutor(
                6,
                6,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                threadFactory,
                (r, executor) -> {
                    try {
                        executor.getQueue().put(r);
                    } catch (InterruptedException ignored) {
                    }
                });
    }

    /**
     * @param timeout
     * @param timeUnit
     * @return
     * @throws MqttException
     */
    public DisconnectPromise disconnect(long timeout, TimeUnit timeUnit) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        if (clientContext.getClientSession().isDisconnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
        }
        if (clientContext.getClientSession().isDisconnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
        }
        clientContext.getClientSession().setTerminated(true);
        MqttDisconnectMessage disconnectMessage = new MqttDisconnectMessage();
        final DisconnectPromiseImpl disconnectPromise = new DisconnectPromiseImpl(executorService);
        Disconnector disconnector = new Disconnector(timeout, timeUnit, workerThreadPoolExecutor, connectOptions.getClientId());
        disconnector.start(sender, disconnectMessage, new PromiseListener() {
            @Override
            public void onSuccess(Promise<?> promise) {
                clientContext.getSessionStore().close();
                disconnectPromise.setResult((MqttDisconnectMessage) promise.getResult());
                closeWorkerThreadPool();
            }

            @Override
            public void onFailure(Promise<?> promise) {
                disconnectPromise.setCause((MqttException) promise.cause());
                closeWorkerThreadPool();
            }
        });
        return disconnectPromise;
    }

    /**
     * when we disconnected the connection, all internal worker is shutdown too,
     * so we should shutdown the worker thread pool now.
     */
    private void closeWorkerThreadPool() {
        workerThreadPoolExecutor.shutdownNow();
    }

    /**
     * @param topic
     * @param payload
     * @param qos
     * @param retained
     * @return
     * @throws MqttException
     */
    public PublishPromise<? extends MqttMessage> publish(String topic, byte[] payload, MqttQoS qos, boolean retained) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, packetId);
        MqttPublishMessage publishMessage = new MqttPublishMessage(fixedHeader, variableHeader, payload);
        PublishPromise<? extends MqttMessage> promise;
        if (qos == MqttQoS.AT_MOST_ONCE) {
            promise = new Qos0PublishPromise(topic, qos, payload, executorService);
        } else if (qos == MqttQoS.AT_LEAST_ONCE) {
            promise = new Qos1PublishPromise(topic, qos, packetId, payload, executorService);
        } else {
            promise = new Qos2PublishPromise(topic, qos, packetId, payload, executorService);
        }
        clientContext.getPromiseKeeper().keep(publishMessage, promise);
        MqttOutboundPublishMessageHandler handler = OutboundMessageHandlerFactory.getHandler(fixedHeader.messageType());
        handler.handle(publishMessage, clientContext);
        return promise;
    }

    /**
     * @param topicFilter
     * @param qos
     * @param listener
     * @return
     * @throws MqttException
     */
    public SubscribePromise subscribe(String topicFilter, MqttQoS qos, MessageListener listener) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        List<MqttTopicSubscription> subscriptions = new ArrayList<>(1);
        MqttTopicSubscription subscription = new MqttTopicSubscription(topicFilter, qos);
        subscriptions.add(subscription);
        MqttSubscribePayload payload = new MqttSubscribePayload(subscriptions);
        MqttSubscribeMessage subscribeMessage = new MqttSubscribeMessage(variableHeader, payload);
        Map<MqttQoS, MessageListener> listenerMap = null;
        if (listener != null) {
            listenerMap = new HashMap<>();
            listenerMap.put(qos, listener);
        }
        Map<String, Map<MqttQoS, MessageListener>> callbacks = new HashMap<>();
        callbacks.put(topicFilter, listenerMap);
        SubscribePromise promise = new SubscribePromiseImpl(callbacks, executorService);
        clientContext.getPromiseKeeper().keep(subscribeMessage, promise);
        MqttSubscribeMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.SUBSCRIBE);
        handler.handle(subscribeMessage, clientContext);
        return promise;
    }

    /**
     * @param topicFilters
     * @param qoses
     * @param listener
     * @return
     * @throws MqttException
     */
    public SubscribePromise subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener listener) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        List<MqttTopicSubscription> subscriptions = new ArrayList<>(topicFilters.length);
        Map<String, Map<MqttQoS, MessageListener>> subs = new HashMap<>(topicFilters.length);
        for (int i = 0; i < topicFilters.length; i++) {
            MqttTopicSubscription subscription = new MqttTopicSubscription(topicFilters[i], qoses[i]);
            subscriptions.add(subscription);
            if (listener != null) {
                Map<MqttQoS, MessageListener> listeners = subs.computeIfAbsent(topicFilters[i], k -> new HashMap<>());
                listeners.put(qoses[i], listener);
            }
        }
        MqttSubscribePayload payload = new MqttSubscribePayload(subscriptions);
        MqttSubscribeMessage subscribeMessage = new MqttSubscribeMessage(variableHeader, payload);
        SubscribePromise promise = new SubscribePromiseImpl(subs, executorService);
        clientContext.getPromiseKeeper().keep(subscribeMessage, promise);
        MqttSubscribeMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.SUBSCRIBE);
        handler.handle(subscribeMessage, clientContext);
        return promise;
    }

    /**
     * @param topicFilters
     * @param qoses
     * @param listeners
     * @return
     * @throws MqttException
     */
    public SubscribePromise subscribe(String[] topicFilters, MqttQoS[] qoses, MessageListener[] listeners) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        List<MqttTopicSubscription> subscriptions = new ArrayList<>(topicFilters.length);
        Map<String, Map<MqttQoS, MessageListener>> subs = new HashMap<>(topicFilters.length);
        for (int i = 0; i < topicFilters.length; i++) {
            MqttTopicSubscription subscription = new MqttTopicSubscription(topicFilters[i], qoses[i]);
            subscriptions.add(subscription);
            if (listeners[i] != null) {
                Map<MqttQoS, MessageListener> listenerMap = subs.computeIfAbsent(topicFilters[i], k -> new HashMap<>());
                listenerMap.put(qoses[i], listeners[i]);
            }
        }
        MqttSubscribePayload payload = new MqttSubscribePayload(subscriptions);
        MqttSubscribeMessage subscribeMessage = new MqttSubscribeMessage(variableHeader, payload);
        SubscribePromise promise = new SubscribePromiseImpl(subs, executorService);
        clientContext.getPromiseKeeper().keep(subscribeMessage, promise);
        MqttSubscribeMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.SUBSCRIBE);
        handler.handle(subscribeMessage, clientContext);
        return promise;
    }

    /**
     * @param topicFilter
     * @return
     * @throws MqttException
     */
    public UnsubscribePromise unsubscribe(String topicFilter) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topicFilter));
        MqttUnsubscribeMessage unsubscribeMessage = new MqttUnsubscribeMessage(variableHeader, payload);
        UnsubscribePromise promise = new UnsubscribePromiseImpl(new String[]{topicFilter}, executorService);
        clientContext.getPromiseKeeper().keep(unsubscribeMessage, promise);
        MqttUnsubscribeMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.UNSUBSCRIBE);
        handler.handle(unsubscribeMessage, clientContext);
        return promise;
    }

    /**
     * @param topicFilters
     * @return
     * @throws MqttException
     */
    public UnsubscribePromise unsubscribe(String[] topicFilters) throws MqttException {
        if (clientContext.getClientSession().isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        int packetId = getPacketId();
        MqttPacketIdVariableHeader variableHeader = new MqttPacketIdVariableHeader(packetId);
        List<String> list = Arrays.asList(topicFilters);
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(list);
        MqttUnsubscribeMessage unsubscribeMessage = new MqttUnsubscribeMessage(variableHeader, payload);
        UnsubscribePromise promise = new UnsubscribePromiseImpl(topicFilters, executorService);
        clientContext.getPromiseKeeper().keep(unsubscribeMessage, promise);
        MqttUnsubscribeMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.UNSUBSCRIBE);
        handler.handle(unsubscribeMessage, clientContext);
        return promise;
    }

    private int getPacketId() throws MqttException {
        return clientContext.getSessionStore().getNextPacketId();
    }

    /**
     *
     */
    public void notifyConnected() {
        if (!connectOptions.isCleanSession()) {
            LOGGER.info("Republishing unfinished messages...");
            republishInProgressMessage();
        }
        clientContext.getClientSession().notifyConnected();
        if (offlineMessageSender == null) {
            offlineMessageSender = new OfflineMessageSender(connectOptions.getClientId(), clientContext.getSessionStore());
        }
        offlineMessageSender.start(executorService);
        if (clientListener != null) {
            executorService.execute(() -> clientListener.onConnected());
        }
    }

    private void republishInProgressMessage() {
        SessionStore sessionStore = clientContext.getSessionStore();
        Set<Integer> republished = new HashSet<>();
        MqttOutboundPublishMessageHandler publishMessageHandler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.PUBLISH);
        if (sessionStore.getWaitingRecCount() > 0) {
            Set<Integer> packets = sessionStore.getWaitingRecPacketIds();
            for (int packetId : packets) {
                MqttPublishMessage message = sessionStore.retrieve(packetId);
                if (message == null) {
                    // @@ if message is not found, just ack it to release the packetId.
                    try {
                        sessionStore.recReceived(packetId);
                    } catch (MqttException ignore) {
                    }
                    sessionStore.inFlightAck(packetId);
                } else {
                    publishMessageHandler.handle(message, clientContext);
                    republished.add(packetId);
                }
            }
        }
        if (sessionStore.getInFlightCount() > 0) {
            Set<Integer> set = sessionStore.getInFlightPacketIds();
            for (int packetId : set) {
                MqttPublishMessage message = sessionStore.retrieve(packetId);
                if (message == null) {
                    // @@ if message is not found, just ack it to release the packetId.
                    sessionStore.inFlightAck(packetId);
                } else {
                    publishMessageHandler.handle(message, clientContext);
                    republished.add(packetId);
                }
            }
        }
        Set<MqttPublishMessage> storedMessages = sessionStore.getAllMessages();
        if (storedMessages != null && !storedMessages.isEmpty()) {
            for (MqttPublishMessage message : storedMessages) {
                int packetId = message.variableHeader().packetId();
                if (message.fixedHeader().qosLevel() == MqttQoS.AT_MOST_ONCE) {
                    sessionStore.remove(packetId);
                    // @@ if qos is "at most once", that means message won't be published again, so we just release the packetId
                    sessionStore.releasePacketId(packetId);
                } else if (!republished.contains(packetId)) {
                    publishMessageHandler.handle(message, clientContext);
                }
            }
        }
    }

    public void notifyPublished(final MqttPublishMessage publishMessage) {
        if (clientListener != null) {
            executorService.execute(() -> clientListener.onPublished(publishMessage));
        }
    }

    private void notifyMessageSent(final MqttMessage message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sent message: {}", message);
        }
        clientContext.getClientSession().notifySent(message);

        OutboundMqttMessageHandler<MqttMessage> messageHandler = OutboundMessageHandlerFactory.getHandler(message.fixedHeader().messageType());
        if (messageHandler != null) {
            messageHandler.onMessageSent(message, clientContext);
        }

        if (clientListener != null) {
            executorService.execute(() -> clientListener.onMessageSent(message));
        }
    }

    private void notifyMessageSendFailed(MqttMessage message, MqttException cause) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Failed to send message: {}", message);
        }
        clientContext.getClientSession().notifySendFailed(message, cause);
    }

    /**
     * @return
     */
    public MqttConnectOptions getConnectOptions() {
        return connectOptions;
    }

    /**
     * @throws MqttException
     */
    public void close(MqttException reason) throws MqttException {
        ClientSession clientSession = clientContext.getClientSession();
        if (clientSession.isConnecting()) {
            throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
        }
        if (clientSession.isClosed()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
        }
        if (clientSession.isDisconnecting()) {
            closePending = true;
        } else if (clientSession.isConnected()) {
            closePending = true;
            disconnect(30, TimeUnit.SECONDS);
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing pending promises.");
            }
            PromiseKeeper promiseKeeper = clientContext.getPromiseKeeper();
            if (!promiseKeeper.isEmpty()) {
                Collection<Promise<?>> promises = promiseKeeper.getPromises();
                for (Promise<?> promise : promises) {
                    @SuppressWarnings("unchecked")
                    AbstractPromise<MqttMessage, MqttException> abstractPromise = (AbstractPromise<MqttMessage, MqttException>) promise;
                    abstractPromise.setCause(reason);
                }
            }
            clientSession.updateState(ConnectionState.CLOSED);
            if (connectOptions.isCleanSession()) {
                clientContext.getSessionStore().clear();
                clientContext.getSessionStore().close();
            }
        }
    }

    /**
     * @param reason
     */
    void shutdown(final MqttException reason) {
        ClientSession clientSession = clientContext.getClientSession();
        clientSession.aquireStateLock();
        try {
            if (clientSession.isDisconnected() || clientSession.isDisconnecting()) {
                return;
            }
            clientSession.updateState(ConnectionState.DISCONNECTING);
        } finally {
            clientSession.releaseStateLock();
        }
        if (reason != null) {
            LOGGER.info("Client: {} is about to shutdown with reason: {}({})", getConnectOptions().getClientId(), reason.getMessage(), reason.getReasonCode(), reason);
        } else {
            LOGGER.info("Client: {} is about to shutdown.", getConnectOptions().getClientId());
        }
        if (sender != null) {
            sender.stop();
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sender is stopped.");
        }
        if (receiver != null) {
            receiver.stop();
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Receiver is stopped.");
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cannot close connection server.", e);
                }
            }
            connection = null;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Connection closed.");
        }
        clientSession.updateState(ConnectionState.DISCONNECTED);
        if (closePending) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("About to close client.");
            }
            try {
                close(reason);
                clientContext.getMessageNotifier().stop();
            } catch (MqttException e) {
                LOGGER.error("Failed to close client session.", e);
            }
        }
        LOGGER.info("Client: {} shutdown.", getConnectOptions().getClientId());
        if (clientListener != null) {
            executorService.execute(() -> clientListener.onDisconnected(reason));
        }
        if (!clientContext.getClientSession().isReconnecting()) {
            startReconnect();
        }
    }

    private void startReconnect() {
        if (connectOptions.isAutomaticReconnect() && !clientContext.getClientSession().isTerminated()) {
            clientContext.getClientSession().setReconnecting(true);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Reconnection is scheduled at {} milliseconds later.", reconnectDelay);
            }
            if (reconnectTimer == null) {
                reconnectTimer = new Timer("MQTT Reconnecter: " + clientContext.getClientSession().getClientId());
            }
            reconnectTimer.schedule(new ReconnectTask(), reconnectDelay);
        }
    }

    private void attemptReconnect() {
        try {
            ConnectPromise promise = connect(serverURI, connectOptions, offlineOptions);
            promise.setPromiseListener(new PromiseListener() {
                @Override
                public void onSuccess(Promise<?> promise) {
                    LOGGER.info("Client: {} is reconnected.", getConnectOptions().getClientId());
                    stopReconnect();
                    if (clientListener != null) {
                        clientListener.onReconnected();
                    }
                }

                @Override
                public void onFailure(Promise<?> promise) {
                    scheduleNextReconnection();
                }
            });
        } catch (Throwable throwable) {
            LOGGER.error("Cannot connect to server: {}", serverURI, throwable);
            if (throwable instanceof MqttException) {
                MqttException reason = (MqttException) throwable;
                int reasonCode = reason.getReasonCode();
                if (reasonCode != MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED && reasonCode != MqttException.REASON_CODE_CONNECT_IN_PROGRESS) {
                    scheduleNextReconnection();
                }
            } else {
                scheduleNextReconnection();
            }
        }
    }

    private void scheduleNextReconnection() {
        if (reconnectDelay < 30000) {
            reconnectDelay += 1000;
        }
        clientContext.getClientSession().aquireStateLock();
        try {
            startReconnect();
        } finally {
            clientContext.getClientSession().releaseStateLock();
        }
    }

    private void stopReconnect() {
        clientContext.getClientSession().aquireStateLock();
        try {
            clientContext.getClientSession().setReconnecting(false);
            if (reconnectTimer != null) {
                reconnectTimer.cancel();
                reconnectTimer = null;
            }
            reconnectDelay = 1000;
        } finally {
            clientContext.getClientSession().releaseStateLock();
        }
    }

    private class ReconnectTask extends TimerTask {
        @Override
        public void run() {
            attemptReconnect();
        }
    }

    private class Connector implements Runnable {

        private MqttConnectMessage connectMessage;
        private final ExecutorService executorService;
        private final String address;
        private final MqttConnectOptions connectOptions;

        Connector(String address, MqttConnectOptions connectOptions, ExecutorService executorService) {
            this.executorService = executorService;
            this.address = address;
            this.connectOptions = connectOptions;
        }

        void start(MqttConnectMessage connectMessage) {
            this.connectMessage = connectMessage;
            clientContext.getMessageNotifier().start();
            executorService.execute(this);
        }

        public void run() {
            long connectTimeout = connectOptions.getConnectionTimeout() * 1000;
            Thread thread = Thread.currentThread();
            String originalName = thread.getName();
            try {
                thread.setName("MQTT Connector: " + connectOptions.getClientId());
                MqttException mqttEx = null;
                try {
                    long l1 = System.currentTimeMillis();
                    connection = ConnectionFactory.getConnection(address, connectOptions);
                    connection.open();
                    long l2 = System.currentTimeMillis();
                    connectTimeout -= (l2 - l1);

                    InboundMessageQueue inboundMessageQueue = new InboundMessageQueue("MQTT Inbound Queue: ", connectOptions.getClientId());
                    inboundMessageQueue.start(executorService);
                    InboundMessageQueue ackMessageQueue = new InboundMessageQueue("MQTT Ack Queue: ", connectOptions.getClientId());
                    ackMessageQueue.start(executorService);
                    receiver = new Receiver(connectOptions.getClientId(), connection.getInputStream(), inboundMessageQueue, ackMessageQueue);
                    receiver.start(executorService);

                    sender = new Sender(connectOptions.getClientId(), connection.getOutputStream());
                    sender.start(executorService);
                    MqttConnectMessageHandler handler = OutboundMessageHandlerFactory.getHandler(MqttMessageType.CONNECT);
                    handler.handle(connectMessage, clientContext);
                } catch (MqttException ex) {
                    mqttEx = ex;
                } catch (Throwable ex) {
                    mqttEx = new MqttException(ex);
                }

                if (mqttEx != null) {
                    ConnectPromiseImpl promise = (ConnectPromiseImpl) clientContext.getPromiseKeeper().remove(connectMessage);
                    promise.setCause(mqttEx);
                    shutdown(mqttEx);
                    return;
                }
                if (connectTimeout <= 0) {
                    return;
                }
                try {
                    Thread.sleep(connectTimeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!clientContext.getClientSession().isConnected()) {
                    ConnectPromiseImpl promise = (ConnectPromiseImpl) clientContext.getPromiseKeeper().remove(connectMessage);
                    if (promise != null) {
                        MqttException reason = new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
                        promise.setCause(reason);
                        shutdown(reason);
                    }
                }
            } finally {
                thread.setName(originalName);
            }
        }
    }

    private class Disconnector implements Runnable {

        private Sender sender;
        private MqttDisconnectMessage disconnectMessage;
        private final ExecutorService executorService;
        private final String threadName;
        private final long timeout;
        private final TimeUnit timeUnit;
        private DisconnectPromise promise;

        Disconnector(long timeout, TimeUnit timeUnit, ExecutorService executorService, String clientId) {
            this.executorService = executorService;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.threadName = "MQTT Disconnector: " + clientId;
        }

        void start(Sender sender, MqttDisconnectMessage disconnectMessage, PromiseListener promiseListener) {
            this.sender = sender;
            this.disconnectMessage = disconnectMessage;
            promise = new DisconnectPromiseImpl(executorService);
            promise.setPromiseListener(promiseListener);
            executorService.execute(this);
        }

        @Override
        public void run() {
            Thread thread = Thread.currentThread();
            String originalName = thread.getName();
            try {
                thread.setName(threadName);
                clientContext.getClientSession().quiesce(timeout, timeUnit);
                clientContext.getPromiseKeeper().keep(disconnectMessage, promise);
                try {
                    sender.sendInternal(disconnectMessage);
                } catch (MqttException ignore) {
                } finally {
                    clientContext.getPromiseKeeper().remove(disconnectMessage);
                    shutdown(null);
                    ((DisconnectPromiseImpl) promise).setResult(null);
                }
            } finally {
                thread.setName(originalName);
            }
        }
    }

    private abstract class Worker implements Runnable {

        boolean running;
        String threadName;
        Future<?> future;
        Thread thread = null;
        final Semaphore runningSemaphore = new Semaphore(1);

        Worker(String threadName) {
            this.threadName = threadName;
        }

        void start(ExecutorService executorService) {
            synchronized (this) {
                if (!running) {
                    running = true;
                    future = executorService.submit(this);
                }
            }
        }

        void stop() {
            synchronized (this) {
                if (future != null) {
                    future.cancel(true);
                }
                if (running) {
                    running = false;
                    if (thread != null) {
                        thread.interrupt();
                        if (!Thread.currentThread().equals(thread)) {
                            try {
                                runningSemaphore.acquire();
                            } catch (InterruptedException ignore) {
                            } finally {
                                runningSemaphore.release();
                            }
                        }
                    }
                }
            }
            thread = null;
        }

        @Override
        public void run() {
            thread = Thread.currentThread();
            String originalName = thread.getName();
            try {
                thread.setName(threadName);
                try {
                    runningSemaphore.acquire();
                } catch (InterruptedException e) {
                    running = false;
                    return;
                }
                MqttException reason = null;
                try {
                    work();
                } catch (MqttException e) {
                    reason = e;
                } catch (Throwable throwable) {
                    reason = new MqttException(throwable);
                }
                runningSemaphore.release();
                if (reason != null) {
                    if (!clientContext.getClientSession().isDisconnecting() && !clientContext.getClientSession().isDisconnected()) {
                        shutdown(reason);
                    }
                }
            } finally {
                thread.setName(originalName);
            }
        }

        abstract void work() throws MqttException;
    }

    private class Sender extends Worker implements Runnable {

        private final MqttOutputStream outputStream;

        Sender(String clientId, OutputStream outputStream) {
            super("MQTT Sender: " + clientId);
            this.outputStream = new MqttOutputStream(outputStream);
        }

        @Override
        void work() throws MqttException {
            SessionStore sessionStore = clientContext.getSessionStore();
            while (running && !clientContext.getClientSession().isQuiescent() && !Thread.interrupted()) {
                if (sessionStore.getInFlightCount() >= connectOptions.getMaxInflight()) {
                    // @@ there must be too many messages, rest for a while
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException ignore) {
                    }
                    continue;
                }
                MqttMessage message = sessionStore.popFromOutboundZone();
                if (message != null) {
                    sendInternal(message);
                }
            }
        }

        void sendInternal(MqttMessage message) throws MqttException {
            try {
                outputStream.write(message);
                outputStream.flush();
                notifyMessageSent(message);
            } catch (IOException e) {
                MqttException cause = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST);
                notifyMessageSendFailed(message, cause);
                throw cause;
            }
        }
    }

    private class Receiver extends Worker implements Runnable {

        private final MqttInputStream inputStream;
        private final InboundMessageQueue inboundMessageQueue;
        private final InboundMessageQueue ackMessageQueue;

        Receiver(String clientId, InputStream inputStream, InboundMessageQueue inboundMessageQueue, InboundMessageQueue ackMessageQueue) {
            super("MQTT Receiver: " + clientId);
            this.inputStream = new MqttInputStream(inputStream);
            this.inboundMessageQueue = inboundMessageQueue;
            this.ackMessageQueue = ackMessageQueue;
        }

        @Override
        void work() throws MqttException {
            while (running && !Thread.interrupted()) {
                try {
                    MqttMessage message = inputStream.readMqtt();
                    if (message != null) {
                        MqttMessageType messageType = message.fixedHeader().messageType();
                        if (messageType == MqttMessageType.PINGRESP || messageType == MqttMessageType.PUBREC
                                || messageType == MqttMessageType.PUBCOMP || messageType == MqttMessageType.SUBACK
                                || messageType == MqttMessageType.UNSUBACK || messageType == MqttMessageType.PUBREL) {
                            ackMessageQueue.add(message);
                        } else {
                            if (!clientContext.getClientSession().isQuiescent()) {
                                inboundMessageQueue.add(message);
                            }
                        }
                        clientContext.getClientSession().notifyReceived(message);
                    }
                } catch (IOException e) {
                    throw new MqttException(MqttException.REASON_CODE_CONNECTION_LOST);
                }
            }
        }
    }

    private class InboundMessageQueue extends Worker {

        private static final int INBOUND_QUEUE_SIZE = 48;
        private final List<MqttMessage> queue = new ArrayList<>(INBOUND_QUEUE_SIZE);
        private final Lock lock = new ReentrantLock();
        private final Condition spaceWaiter = lock.newCondition();
        private final Condition newMessageWaiter = lock.newCondition();

        InboundMessageQueue(String threadName, String clientId) {
            super(threadName + clientId);
        }

        @Override
        void work() {
            while (running && !clientContext.getClientSession().isQuiescent() && !Thread.interrupted()) {
                MqttMessage message = pop();
                if (message != null) {
                    InboundMqttMessageHandler<MqttMessage> handler = InboundMessageHandlerFactory.getHandler(message.fixedHeader().messageType());
                    handler.handle(message, clientContext);
                }
            }
        }

        void add(final MqttMessage message) {
            lock.lock();
            try {
                while (running && !clientContext.getClientSession().isQuiescent() && queue.size() >= INBOUND_QUEUE_SIZE && !Thread.interrupted()) {
                    try {
                        spaceWaiter.await(200, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                MqttMessageType messageType = message.fixedHeader().messageType();
                if (messageType == MqttMessageType.PINGRESP) {
                    queue.add(0, message);
                } else {
                    queue.add(message);
                }
                if (clientListener != null) {
                    executorService.execute(() -> clientListener.onMessageReceived(message));
                }
                newMessageWaiter.signalAll();
            } finally {
                lock.unlock();
            }
        }

        MqttMessage pop() {
            lock.lock();
            try {
                while (running && queue.isEmpty() && !Thread.interrupted()) {
                    try {
                        newMessageWaiter.await(200, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                MqttMessage message = queue.remove(0);
                spaceWaiter.signalAll();
                return message;
            } finally {
                lock.unlock();
            }
        }
    }


    private class OfflineMessageSender extends Worker {

        private final SessionStore sessionStore;

        OfflineMessageSender(String clientId, SessionStore sessionStore) {
            super("MQTT Offline Sender: " + clientId);
            this.sessionStore = sessionStore;
        }

        @Override
        void work() {
            while (running && !clientContext.getClientSession().isQuiescent() && !Thread.interrupted()) {
                MqttMessage message = sessionStore.popFromOfflineZone();
                if (message == null) {
                    break;
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Move message from offline zone to outbound zone: {}", message);
                }
                sessionStore.appendToOutboundZone(message);
            }
        }
    }
}
