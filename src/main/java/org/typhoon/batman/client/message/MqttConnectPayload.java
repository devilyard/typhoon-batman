/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnectPayload.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

/**
 * @author C.
 */
public final class MqttConnectPayload {

    private final String clientIdentifier;
    private final String willTopic;
    private final byte[] willMessage;
    private final String userName;
    private final byte[] password;

    public MqttConnectPayload(String clientIdentifier, String willTopic, byte[] willMessage, String userName, byte[] password) {
        this.clientIdentifier = clientIdentifier;
        this.willTopic = willTopic;
        this.willMessage = willMessage;
        this.userName = userName;
        this.password = password;
    }

    public String clientIdentifier() {
        return clientIdentifier;
    }

    public String willTopic() {
        return willTopic;
    }

    public byte[] willMessageInBytes() {
        return willMessage;
    }

    public String userName() {
        return userName;
    }

    public byte[] passwordInBytes() {
        return password;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[clientIdentifier=" + clientIdentifier +
                ", willTopic=" + willTopic +
                ", willMessage=" + willMessage +
                ", userName=" + userName +
                ", password=" + password +
                ']';
    }
}
