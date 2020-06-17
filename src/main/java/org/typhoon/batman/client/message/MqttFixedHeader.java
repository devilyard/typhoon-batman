/*
 * Copyright (c) 2019. All rights reserved.
 * MqttFixedHeader.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.message;

import org.typhoon.batman.MqttQoS;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#fixed-header">MQTTV3.1/fixed-header</a>
 *
 * @author C.
 */
public final class MqttFixedHeader {

    private final MqttMessageType messageType;
    private final boolean isDup;
    /**
     * Sets the quality of service for this message.
     * <ul>
     * <li>Quality of Service 0 - indicates that a message should
     * be delivered at most once (zero or one times).  The message will not be persisted to disk,
     * and will not be acknowledged across the network.  This QoS is the fastest,
     * but should only be used for messages which are not valuable - note that
     * if the server cannot process the message (for example, there
     * is an authorization problem), then the delivery is just done.
     * Also known as "fire and forget".</li>
     *
     * <li>Quality of Service 1 - indicates that a message should
     * be delivered at least once (one or more times).  The message can only be delivered safely if
     * it can be persisted, so the application must supply a means of persistence.
     * If a persistence mechanism is not specified, the message will not be
     * delivered in the event of a client failure.
     * The message will be acknowledged across the network.
     * This is the default QoS.</li>
     *
     * <li>Quality of Service 2 - indicates that a message should
     * be delivered once.  The message will be persisted to disk, and will
     * be subject to a two-phase acknowledgement across the network.
     * The message can only be delivered safely if
     * it can be persisted, so the application must supply a means of persistence.
     * If a persistence mechanism is not specified, the message will not be
     * delivered in the event of a client failure.</li>
     *
     * </ul>
     * If persistence is not configured, QoS 1 and 2 messages will still be delivered
     * in the event of a network or server problem as the client will hold state in memory.
     * If the MQTT client is shutdown or fails and persistence is not configured then
     * delivery of QoS 1 and 2 messages can not be maintained as client-side state will
     * be lost.
     */
    private final MqttQoS qosLevel;
    /**
     * Whether or not the publish message should be retained by the messaging engine.
     * Sending a message with retained set to <code>true</code> and with an empty
     * byte array as the payload e.g. <code>new byte[0]</code> will clear the
     * retained message from the server.  The default value is <code>false</code>
     */
    private final boolean isRetain;
    private final int remainingLength;

    public MqttFixedHeader(MqttMessageType messageType, boolean isDup, MqttQoS qosLevel, boolean isRetain, int remainingLength) {
        if (messageType == null) {
            throw new NullPointerException("messageType is null");
        }
        this.messageType = messageType;
        this.isDup = isDup;
        if (qosLevel == null) {
            throw new NullPointerException("qosLevel is null");
        }
        this.qosLevel = qosLevel;
        this.isRetain = isRetain;
        this.remainingLength = remainingLength;
    }

    public MqttMessageType messageType() {
        return messageType;
    }

    public boolean isDup() {
        return isDup;
    }

    public MqttQoS qosLevel() {
        return qosLevel;
    }

    public boolean isRetain() {
        return isRetain;
    }

    public int remainingLength() {
        return remainingLength;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[messageType=" + messageType +
                ", isDup=" + isDup +
                ", qosLevel=" + qosLevel +
                ", isRetain=" + isRetain +
                ", remainingLength=" + remainingLength +
                ']';
    }
}
