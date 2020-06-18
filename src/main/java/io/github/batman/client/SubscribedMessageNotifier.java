/*
 * Copyright (c) 2019. All rights reserved.
 * SubscribedMessageNotifier.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import io.github.batman.client.message.MqttPublishMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author C.
 */
public class SubscribedMessageNotifier {

    private final Map<Topic, MessageListener> listeners = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private ExecutorService executorService;

    public void start() {
        if (executorService == null) {
            synchronized (this) {
                if (executorService == null) {
                    int poolSize = Runtime.getRuntime().availableProcessors() + 1;
                    ThreadFactory threadFactory = new ThreadFactory() {

                        private final AtomicLong threadNumber = new AtomicLong(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            String namePrefix = "pool-message-notifier-";
                            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                            if (!t.isDaemon()) {
                                t.setDaemon(true);
                            }
                            if (t.getPriority() != Thread.NORM_PRIORITY) {
                                t.setPriority(Thread.NORM_PRIORITY);
                            }
                            return t;
                        }
                    };
                    executorService = new ScheduledThreadPoolExecutor(poolSize, threadFactory, (r, executor) -> {
                        try {
                            executor.getQueue().put(r);
                        } catch (InterruptedException ignore) {
                        }
                    });
                }
            }
        }
    }

    public void stop() {
        if (executorService != null) {
            synchronized (this) {
                if (executorService != null) {
                    executorService.shutdown();
                    executorService = null;
                }
            }
        }
    }

    /**
     * @param topicFilter
     * @param listener
     */
    public void addListener(Topic topicFilter, MessageListener listener) {
        lock.writeLock().lock();
        try {
            listeners.put(topicFilter, listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param topicFilter
     */
    public void removeListener(Topic topicFilter) {
        lock.writeLock().lock();
        try {
            listeners.remove(topicFilter);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Notify the message listeners when publish message received.
     *
     * @param publishMessage
     */
    public void notifyMessage(final MqttPublishMessage publishMessage) {
        final Topic topicName = new Topic(publishMessage.variableHeader().topicName());
        lock.readLock().lock();
        try {
            if (!listeners.isEmpty()) {
                for (Map.Entry<Topic, MessageListener> entry : listeners.entrySet()) {
                    final Topic topicFilter = entry.getKey();
                    final MessageListener listener = entry.getValue();
                    executorService.execute(() -> {
                        if (topicFilter.match(topicName)) {
                            listener.onMessage(publishMessage);
                        }
                    });
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
