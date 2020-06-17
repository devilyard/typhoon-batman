/*
 * Copyright (c) 2019. All rights reserved.
 * ClientContext.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import org.typhoon.batman.client.store.SessionStore;

/**
 * @author C.
 */
public class ClientContext {

    private ClientSession clientSession;
    private SessionStore sessionStore;
    private PromiseKeeper promiseKeeper;
    private MessageTransporter messageTransporter;
    private IMqttAsyncClient client;
    private SubscribedMessageNotifier messageNotifier;

    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public PromiseKeeper getPromiseKeeper() {
        return promiseKeeper;
    }

    public void setPromiseKeeper(PromiseKeeper promiseKeeper) {
        this.promiseKeeper = promiseKeeper;
    }

    public MessageTransporter getMessageTransporter() {
        return messageTransporter;
    }

    public void setMessageTransporter(MessageTransporter messageTransporter) {
        this.messageTransporter = messageTransporter;
    }

    public IMqttAsyncClient getClient() {
        return client;
    }

    public void setClient(IMqttAsyncClient client) {
        this.client = client;
    }

    public SubscribedMessageNotifier getMessageNotifier() {
        return messageNotifier;
    }

    public void setMessageNotifier(SubscribedMessageNotifier messageNotifier) {
        this.messageNotifier = messageNotifier;
    }
}
