/*
 * Copyright (c) 2019. All rights reserved.
 * MqttException.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

import java.util.ResourceBundle;

/**
 * See <a href="https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_7.5.0/com.ibm.mq.tro.doc/q039390_.htm">MQTT v3 Java client reason codes</a>
 *
 * @author C.
 */
public class MqttException extends Exception {

    public static final short REASON_CODE_CLIENT_EXCEPTION = 0x00;
    public static final short REASON_CODE_INVALID_PROTOCOL_VERSION = 0x01;
    public static final short REASON_CODE_INVALID_CLIENT_ID = 0x02;
    public static final short REASON_CODE_BROKER_UNAVAILABLE = 0x03;
    public static final short REASON_CODE_FAILED_AUTHENTICATION = 0x04;
    public static final short REASON_CODE_NOT_AUTHORIZED = 0x05;
    public static final short REASON_CODE_UNEXPECTED_ERROR = 0x06;
    public static final short REASON_CODE_INVALID_TOPIC = 0x07;
    public static final short REASON_CODE_SUBSCRIBE_FAILED = 0x80;

    public static final short REASON_CODE_CLIENT_TIMEOUT = 32000;
    public static final short REASON_CODE_NO_PACKET_IDS_AVAILABLE = 32001;
    public static final short REASON_CODE_WRITE_TIMEOUT = 32002;
    public static final short REASON_CODE_CLIENT_ALREADY_CONNECTED = 32100;
    public static final short REASON_CODE_CLIENT_ALREADY_DISCONNECTED = 32101;
    public static final short REASON_CODE_CLIENT_DISCONNECTING = 32102;
    public static final short REASON_CODE_SERVER_CONNECT_ERROR = 32103;
    public static final short REASON_CODE_SOCKET_FACTORY_MISMATCH = 32105;
    public static final short REASON_CODE_SSL_CONFIG_ERROR = 32106;

    public static final short REASON_CODE_INVALID_MESSAGE = 32108;
    public static final short REASON_CODE_CONNECTION_LOST = 32109;
    public static final short REASON_CODE_CONNECT_IN_PROGRESS = 32110;
    public static final short REASON_CODE_CLIENT_CLOSED = 32111;
    public static final short REASON_CODE_MAX_INFLIGHT = 32202;
    public static final short REASON_CODE_OFFLINE_BUFFER_FULL = 32203;

    private short reasonCode = REASON_CODE_CLIENT_EXCEPTION;

    private final ResourceBundle bundle = ResourceBundle.getBundle("META-INF/message");

    public MqttException(short reasonCode) {
        super();
        this.reasonCode = reasonCode;
    }

    public MqttException(Throwable cause) {
        super(cause);
    }

    public MqttException(short reasonCode, Throwable cause) {
        super(cause);
        this.reasonCode = reasonCode;
    }

    public short getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getMessage() {
        return bundle.getString(String.valueOf(reasonCode));
    }

    @Override
    public String toString() {
        return  super.toString() + "(" + getReasonCode() + ")";
    }
}
