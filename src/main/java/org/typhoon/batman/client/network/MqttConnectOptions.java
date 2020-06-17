/*
 * Copyright (c) 2019. All rights reserved.
 * MqttConnectOptions.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */

package org.typhoon.batman.client.network;

import org.typhoon.batman.MqttQoS;
import org.typhoon.batman.client.TopicToken;
import org.typhoon.batman.client.message.*;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import java.util.Properties;

/**
 * Holds the set of options that control how the client connects to a server.
 *
 * @author C.
 */
public class MqttConnectOptions {
    /**
     * The default keep alive interval in seconds if one is not specified
     */
    public static final int KEEP_ALIVE_INTERVAL_DEFAULT = 60;
    /**
     * The default connection timeout in seconds if one is not specified
     */
    public static final int CONNECTION_TIMEOUT_DEFAULT = 30;
    /**
     * The default max inflight if one is not specified
     */
    public static final int MAX_INFLIGHT_DEFAULT = 10;
    /**
     * The default clean session setting if one is not specified
     */
    public static final boolean CLEAN_SESSION_DEFAULT = true;

    private int keepAliveInterval = KEEP_ALIVE_INTERVAL_DEFAULT;
    private int maxInflight = MAX_INFLIGHT_DEFAULT;
    private MqttPublishMessage willMessage = null;
    private String username;
    private byte[] password;
    private String clientId;
    private SocketFactory socketFactory;
    private Properties sslClientProps = null;
    private HostnameVerifier sslHostnameVerifier = null;
    private boolean cleanSession = CLEAN_SESSION_DEFAULT;
    private int connectionTimeout = CONNECTION_TIMEOUT_DEFAULT;
    private MqttVersion mqttVersion = null;
    private boolean automaticReconnect = false;

    /**
     * Constructs a new <code>MqttConnectOptions</code> object using the
     * default values.
     * <p>
     * The defaults are:
     * <ul>
     * <li>The keepalive interval is 60 seconds</li>
     * <li>Clean Session is true</li>
     * <li>The message delivery retry interval is 15 seconds</li>
     * <li>The connection timeout period is 30 seconds</li>
     * <li>No Will message is set</li>
     * <li>A standard SocketFactory is used</li>
     * </ul>
     * More information about these values can be found in the setter methods.
     */
    public MqttConnectOptions() {
    }

    /**
     * Returns the password to use for the connection.
     *
     * @return the password to use for the connection.
     */
    public byte[] getPassword() {
        return password;
    }

    /**
     * Sets the password to use for the connection.
     *
     * @param password a byte array of the password
     */
    public void setPassword(byte[] password) {
        this.password = password;
    }

    /**
     * Returns the user name to use for the connection.
     *
     * @return the user name to use for the connection.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user name to use for the connection.
     *
     * @param username the username as a String
     * @throws IllegalArgumentException if the username is blank or only contains whitespace characters.
     */
    public void setUsername(String username) {
        if ((username != null) && (username.trim().equals(""))) {
            throw new IllegalArgumentException("username");
        }
        this.username = username;
    }

    /**
     * Returns the client id to use for the connection.
     *
     * @return the client id to use for the connection.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client id to use for the connection.
     *
     * @param clientId the client id to use for the connection.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Sets the "Last Will and Testament" (LWT) for the connection.
     * In the event that this client unexpectedly loses its connection to the
     * server, the server will publish a message to itself using the supplied
     * details.
     *
     * @param topic    the topic to publish to.
     * @param payload  the byte payload for the message.
     * @param qos      the quality of service to publish the message at (0, 1 or 2).
     * @param retained whether or not the message should be retained.
     */
    public void setWill(String topic, byte[] payload, MqttQoS qos, boolean retained) {
        if (topic == null) {
            throw new IllegalArgumentException("topic");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload");
        }
        if (topic.contains(TopicToken.MULTI.toString()) || topic.contains(TopicToken.SINGLE.toString())) {
            throw new IllegalArgumentException("Topic name should not contain wildcard.");
        }
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, 0);
        this.willMessage = new MqttPublishMessage(fixedHeader, variableHeader, payload);
    }

    /**
     * Returns the "keep alive" interval.
     *
     * @return the keep alive interval.
     * @see #setKeepAliveInterval(int)
     */
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Sets the "keep alive" interval.
     * This value, measured in seconds, defines the maximum time interval
     * between messages sent or received. It enables the client to
     * detect if the server is no longer available, without
     * having to wait for the TCP/IP timeout. The client will ensure
     * that at least one message travels across the network within each
     * keep alive period.  In the absence of a data-related message during
     * the time period, the client sends a very small "ping" message, which
     * the server will acknowledge.
     * A value of 0 disables keepalive processing in the client.
     * <p>The default value is 60 seconds</p>
     *
     * @param keepAliveInterval the interval, measured in seconds, must be &gt;= 0.
     * @throws IllegalArgumentException if the keepAliveInterval was invalid
     */
    public void setKeepAliveInterval(int keepAliveInterval) throws IllegalArgumentException {
        if (keepAliveInterval < 0) {
            throw new IllegalArgumentException();
        }
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * Returns the "max inflight".
     * The max inflight limits to how many messages we can send without receiving acknowledgments.
     *
     * @return the max inflight
     * @see #setMaxInflight(int)
     */
    public int getMaxInflight() {
        return maxInflight;
    }

    /**
     * Sets the "max inflight".
     * please increase this value in a high traffic environment.
     * <p>The default value is 10</p>
     *
     * @param maxInflight the number of maxInfligt messages
     */
    public void setMaxInflight(int maxInflight) {
        if (maxInflight < 0) {
            throw new IllegalArgumentException();
        }
        this.maxInflight = maxInflight;
    }

    /**
     * Returns the connection timeout value.
     *
     * @return the connection timeout value.
     * @see #setConnectionTimeout(int)
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout value.
     * This value, measured in seconds, defines the maximum time interval
     * the client will wait for the network connection to the MQTT server to be established.
     * The default timeout is 30 seconds.
     * A value of 0 disables timeout processing meaning the client will wait until the
     * network connection is made successfully or fails.
     *
     * @param connectionTimeout the timeout value, measured in seconds. It must be &gt;0;
     */
    public void setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout < 0) {
            throw new IllegalArgumentException();
        }
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Returns the socket factory that will be used when connecting, or
     * <code>null</code> if one has not been set.
     *
     * @return The Socket Factory
     */
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Sets the <code>SocketFactory</code> to use.  This allows an application
     * to apply its own policies around the creation of network sockets.  If
     * using an SSL connection, an <code>SSLSocketFactory</code> can be used
     * to supply application-specific security settings.
     *
     * @param socketFactory the factory to use.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * Returns the message to be sent as last will and testament (LWT).
     * The returned object is "read only".  Calling any "setter" methods on
     * the returned object will result in an
     * <code>IllegalStateException</code> being thrown.
     *
     * @return the message to use, or <code>null</code> if LWT is not set.
     */
    public MqttPublishMessage getWillMessage() {
        return willMessage;
    }

    /**
     * Returns the SSL properties for the connection.
     *
     * @return the properties for the SSL connection
     */
    public Properties getSSLProperties() {
        return sslClientProps;
    }

    /**
     * Sets the SSL properties for the connection.
     * <p>Note that these
     * properties are only valid if an implementation of the Java
     * Secure Socket Extensions (JSSE) is available.  These properties are
     * <em>not</em> used if a SocketFactory has been set using
     * {@link #setSocketFactory(SocketFactory)}.
     * The following properties can be used:</p>
     * <dl>
     * <dt>org.typhoon.ssl.protocol</dt>
     * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
     * <dt>org.typhoon.ssl.contextProvider
     * <dd>Underlying JSSE provider.  For example "SunJSSE"</dd>
     *
     * <dt>javax.net.ssl.keyStore</dt>
     * <dd>The name of the file that contains the KeyStore object that you
     * want the KeyManager to use. For example /mydir/etc/key.p12</dd>
     *
     * <dt>javax.net.ssl.keyStorePassword</dt>
     * <dd>The password for the KeyStore object that you want the KeyManager to use.</dd>
     *
     * <dt>javax.net.ssl.keyStoreType</dt>
     * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
     *
     * <dt>org.typhoon.ssl.keyStoreProvider</dt>
     * <dd>Key store provider.</dd>
     *
     * <dt>javax.net.ssl.trustStore</dt>
     * <dd>The name of the file that contains the KeyStore object that you
     * want the TrustManager to use.</dd>
     *
     * <dt>javax.net.ssl.trustStorePassword</dt>
     * <dd>The password for the TrustStore object that you want the
     * TrustManager to use.</dd>
     *
     * <dt>javax.net.ssl.trustStoreType</dt>
     * <dd>The type of KeyStore object that you want the default TrustManager to use.
     * Same possible values as "keyStoreType".</dd>
     *
     * <dt>org.typhoon.ssl.trustStoreProvider</dt>
     * <dd>Trust store provider.</dd>
     *
     * <dt>org.typhoon.ssl.enabledCipherSuites</dt>
     * <dd>A list of which ciphers are enabled.  Values are dependent on the provider,
     * for example: SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
     *
     * <dt>ssl.KeyManagerFactory.algorithm</dt>
     * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory object
     * instead of using the default algorithm available in the platform.
     * </dd>
     *
     * <dt>ssl.TrustManagerFactory.algorithm</dt>
     * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory object
     * instead of using the default algorithm available in the platform.
     * </dd>
     * </dl>
     *
     * @param props The SSL {@link Properties}
     */
    public void setSSLProperties(Properties props) {
        this.sslClientProps = props;
    }

    /**
     * Returns the HostnameVerifier for the SSL connection.
     *
     * @return the HostnameVerifier for the SSL connection
     */
    public HostnameVerifier getSSLHostnameVerifier() {
        return sslHostnameVerifier;
    }

    /**
     * Sets the HostnameVerifier for the SSL connection. Note that it will be
     * used after handshake on a connection and you should do actions by
     * yourself when hostname is verified error.
     * <p>
     * There is no default HostnameVerifier
     * </p>
     *
     * @param hostnameVerifier the {@link HostnameVerifier}
     */
    public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.sslHostnameVerifier = hostnameVerifier;
    }

    /**
     * Returns whether the client and server should remember state for the client across reconnects.
     *
     * @return the clean session flag
     */
    public boolean isCleanSession() {
        return this.cleanSession;
    }

    /**
     * Sets whether the client and server should remember state across restarts and reconnects.
     * <ul>
     * <li>If set to false both the client and server will maintain state across
     * restarts of the client, the server and the connection. As state is maintained:
     * <ul>
     * <li>Message delivery will be reliable meeting
     * the specified QOS even if the client, server or connection are restarted.
     * <li> The server will treat a subscription as durable.
     * </ul>
     * <li>If set to true the client and server will not maintain state across
     * restarts of the client, the server or the connection. This means
     * <ul>
     * <li>Message delivery to the specified QOS cannot be maintained if the
     * client, server or connection are restarted
     * <li>The server will treat a subscription as non-durable
     * </ul>
     * </ul>
     *
     * @param cleanSession Set to True to enable cleanSession
     */
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * Sets the MQTT version.
     * The default action is to connect with version 3.1.1,
     * and to fall back to 3.1 if that fails.
     * Version 3.1.1 or 3.1 can be selected specifically, with no fall back.
     *
     * @param mqttVersion the version of the MQTT protocol.
     * @throws IllegalArgumentException If the MqttVersion supplied is invalid
     */
    public void setMqttVersion(MqttVersion mqttVersion) throws IllegalArgumentException {
        this.mqttVersion = mqttVersion;
    }


    /**
     * Returns the MQTT version.
     *
     * @return the MQTT version.
     * @see #setMqttVersion(MqttVersion)
     */
    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    /**
     * Returns whether the client will automatically attempt to reconnect to the
     * server if the connection is lost
     *
     * @return the automatic reconnection flag.
     */
    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    /**
     * Sets whether the client will automatically attempt to reconnect to the
     * server if the connection is lost.
     * <ul>
     * <li>If set to false, the client will not attempt to automatically
     * reconnect to the server in the event that the connection is lost.</li>
     * <li>If set to true, in the event that the connection is lost, the client
     * will attempt to reconnect to the server. It will initially wait 1 second before
     * it attempts to reconnect, for every failed reconnect attempt, the delay will double
     * until it is at 2 minutes at which point the delay will stay at 2 minutes.</li>
     * </ul>
     *
     * @param automaticReconnect If set to True, automatic reconnect will be enabled
     */
    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

}
