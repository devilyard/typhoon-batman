/*
 * Copyright (c) 2019. All rights reserved.
 * SSLSocketFactoryFactory.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.network;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.Set;

/**
 * @author C.
 */
public class SSLSocketFactoryFactory {
    /**
     * Property keys specific to the client.
     */
    public static final String SSL_PROTOCOL = "org.typhoon.ssl.protocol";
    public static final String SSL_CONTEXT_PROVIDER = "org.typhoon.ssl.contextProvider";
    public static final String KEY_STORE_PROVIDER = "org.typhoon.ssl.keyStoreProvider";
    public static final String TRUST_STORE_PROVIDER = "org.typhoon.ssl.trustStoreProvider";
    public static final String CIPHER_SUITES = "org.typhoon.ssl.enabledCipherSuites";
    public static final String CLIENT_AUTH = "org.typhoon.ssl.clientAuthentication";

    /**
     * Property keys used for java system properties
     */
    public static final String SYS_KEY_STORE = "javax.net.ssl.keyStore";
    public static final String SYS_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
    public static final String SYS_KEYSTORE_PWD = "javax.net.ssl.keyStorePassword";
    public static final String SYS_TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String SYS_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    public static final String SYS_TRUST_STORE_PWD = "javax.net.ssl.trustStorePassword";
    public static final String SYS_KEY_MGR_ALGORITHM = "ssl.KeyManagerFactory.algorithm";
    public static final String SYS_TRUST_MGR_ALGORITHM = "ssl.TrustManagerFactory.algorithm";

    public static final String DEFAULT_PROTOCOL = "TLS";

    private static final String[] propertyKeys = {SSL_PROTOCOL, SSL_CONTEXT_PROVIDER, KEY_STORE_PROVIDER, TRUST_STORE_PROVIDER, CIPHER_SUITES, CLIENT_AUTH};

    private Properties defaultProperties;

    /**
     * Checks whether a key belongs to the supported customize SSL property keys.
     *
     * @param key
     * @return whether a key belongs to the supported customize SSL property keys.
     */
    private boolean keyValid(String key) {
        for (String definedKey : propertyKeys) {
            if (definedKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the property keys belong to the supported customize SSL property key set.
     *
     * @param properties
     * @throws IllegalArgumentException if any of the properties is not a valid customize SSL property key.
     */
    private void checkPropertyKeys(Properties properties) throws IllegalArgumentException {
        Set<Object> keys = properties.keySet();
        for (Object key : keys) {
            String k = (String) key;
            if (!keyValid(k)) {
                throw new IllegalArgumentException(k + " is not a valid customize SSL property key.");
            }
        }
    }

    /**
     * Converts a string of cipher names into an array of cipher names
     *
     * @param ciphers A list of ciphers, separated by comma.
     * @return An array of string, each string containing a single cipher name.
     */
    public static String[] unpackCipherSuites(String ciphers) {
        return ciphers.split(",");
    }

    /**
     * Initializes the SSLSocketFactoryFactory with the provided properties for
     * the provided configuration.
     *
     * @param props A properties object containing customize SSL properties that are
     *              qualified by one or more configuration identifiers.
     * @throws IllegalArgumentException if any of the properties is not a valid customize SSL property key.
     */
    public void initialize(Properties props) throws IllegalArgumentException {
        checkPropertyKeys(props);
        // copy the properties.
        Properties p = new Properties();
        p.putAll(props);
        this.defaultProperties = p;
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     */
    private String getProperty(String key, String defaultValue) {
        return defaultProperties.getProperty(key, defaultValue);
    }

    /**
     * Gets the SSL protocol variant of the indicated configuration or the
     * default configuration.
     *
     * @return The SSL protocol variant.
     */
    public String getSSLProtocol() {
        return getProperty(SSL_PROTOCOL, DEFAULT_PROTOCOL);
    }

    /**
     * Gets the SSL context provider of the indicated configuration
     *
     * @return The SSL Context provider.
     */
    public String getSSLContextProvider() {
        return getProperty(SSL_CONTEXT_PROVIDER, null);
    }

    /**
     * Gets the plain-text password that is used for the keystore.
     *
     * @return The password in plain text.
     */
    public char[] getKeyStorePassword() {
        String pw = getProperty(SYS_KEYSTORE_PWD, null);
        char[] r = null;
        if (pw != null) {
            r = pw.toCharArray();
        }
        return r;
    }

    public String getKeyStore() {
        return getProperty(SYS_KEY_STORE, null);
    }

    /**
     * Gets the type of keystore.
     *
     * @return The keystore type.
     */
    public String getKeyStoreType() {
        return getProperty(SYS_KEYSTORE_TYPE, KeyStore.getDefaultType());
    }

    /**
     * Gets the keystore provider.
     *
     * @return The name of the keystore provider.
     */
    public String getKeyStoreProvider() {
        return getProperty(KEY_STORE_PROVIDER, null);
    }

    /**
     * Gets the key manager algorithm that is used.
     *
     * @return The key manager algorithm.
     */
    public String getKeyManager() {
        return getProperty(SYS_KEY_MGR_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
    }

    /**
     * Gets the name of the truststore file that is used.
     *
     * @return The name of the file that contains the truststore.
     */
    public String getTrustStore() {
        return getProperty(SYS_TRUST_STORE, null);
    }

    /**
     * Gets the plain-text password that is used for the truststore.
     *
     * @return The password in plain text.
     */
    public char[] getTrustStorePassword() {
        String pw = getProperty(SYS_TRUST_STORE_PWD, null);
        char[] r = null;
        if (pw != null) {
            r = pw.toCharArray();
        }
        return r;
    }

    /**
     * Gets the type of truststore.
     *
     * @return The truststore type.
     */
    public String getTrustStoreType() {
        return getProperty(SYS_TRUST_STORE_TYPE, KeyStore.getDefaultType());
    }

    /**
     * Gets the truststore provider.
     *
     * @return The name of the truststore provider.
     */
    public String getTrustStoreProvider() {
        return getProperty(TRUST_STORE_PROVIDER, null);
    }

    /**
     * Gets the trust manager algorithm that is used.
     *
     * @return The trust manager algorithm.
     */
    public String getTrustManager() {
        return getProperty(SYS_TRUST_MGR_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
    }

    /**
     * Returns an array with the enabled ciphers.
     *
     * @return an array with the enabled ciphers
     */
    public String[] getEnabledCipherSuites() {
        String ciphers = getProperty(CIPHER_SUITES, null);
        return unpackCipherSuites(ciphers);
    }

    /**
     * @return An SSL context factory.
     * @throws MqttSecurityException
     */
    private SSLContext getSSLContext() throws MqttSecurityException {
        try {
            // @@ to get keyManagers
            String keyStoreName = getKeyStore();
            String keyStoreType = getKeyStoreType();
            String keyManager = getKeyManager();

            KeyManager[] keyManagers = null;
            if (keyStoreName != null && keyStoreType != null && keyManager != null) {
                try {
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                    char[] keyStorePwd = getKeyStorePassword();
                    keyStore.load(new FileInputStream(keyStoreName), keyStorePwd);
                    String keyStoreProvider = getKeyStoreProvider();
                    KeyManagerFactory keyManagerFactory;
                    if (keyStoreProvider != null) {
                        keyManagerFactory = KeyManagerFactory.getInstance(keyManager, keyStoreProvider);
                    } else {
                        keyManagerFactory = KeyManagerFactory.getInstance(keyManager);
                    }
                    keyManagerFactory.init(keyStore, keyStorePwd);
                    keyManagers = keyManagerFactory.getKeyManagers();
                } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException e) {
                    throw new MqttSecurityException(e);
                }
            }
            // @@ to get trustManagers
            String trustStoreName = getTrustStore();
            String trustStoreType = getTrustStoreType();
            String trustManager = getTrustManager();

            TrustManager[] trustManagers = null;
            if (trustStoreName != null && trustStoreType != null && trustManager != null) {
                try {
                    KeyStore trustStore = KeyStore.getInstance(trustStoreType);
                    char[] trustStorePwd = getTrustStorePassword();
                    trustStore.load(new FileInputStream(trustStoreName), trustStorePwd);
                    String trustStoreProvider = getTrustStoreProvider();
                    TrustManagerFactory trustManagerFactory;
                    if (trustStoreProvider != null) {
                        trustManagerFactory = TrustManagerFactory.getInstance(trustManager, trustStoreProvider);
                    } else {
                        trustManagerFactory = TrustManagerFactory.getInstance(trustManager);
                    }
                    trustManagerFactory.init(trustStore);
                    trustManagers = trustManagerFactory.getTrustManagers();
                } catch (KeyStoreException | CertificateException | IOException e) {
                    throw new MqttSecurityException(e);
                }
            }

            SSLContext ctx;
            String protocol = getSSLProtocol();
            String provider = getSSLContextProvider();
            if (provider == null) {
                ctx = SSLContext.getInstance(protocol);
            } else {
                ctx = SSLContext.getInstance(protocol, provider);
            }
            ctx.init(keyManagers, trustManagers, null);
            return ctx;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyManagementException e) {
            throw new MqttSecurityException(e);
        }
    }

    /**
     * Returns an SSL socket factory for the given configuration. If no SSLProtocol is set, uses DEFAULT_PROTOCOL.
     *
     * @return SSLSocketFactory
     * @throws MqttSecurityException if an error occurs whilst creating the {@link SSLSocketFactory}
     * @see SSLSocketFactoryFactory#DEFAULT_PROTOCOL
     */
    public SSLSocketFactory createSocketFactory() throws MqttSecurityException {
        SSLContext ctx = getSSLContext();
        return ctx.getSocketFactory();
    }

}
