/*
 * Copyright (c) 2019. All rights reserved.
 * SessionStore.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client.store;

import io.github.batman.client.MqttException;
import io.github.batman.client.OfflineOptions;
import io.github.batman.client.message.MqttMessage;
import io.github.batman.client.message.MqttPublishMessage;

import java.util.Set;

/**
 * @author C.
 */
public interface SessionStore {

    /**
     *
     * @param serverURI
     * @param clientId
     * @throws MqttException
     */
    void open(String serverURI, String clientId) throws MqttException;

    /**
     *
     */
    void close();

    /**
     *
     */
    void clear();

    /**
     * save a publish message.
     *
     * @param message the message to save
     */
    void store(MqttPublishMessage message);

    /**
     * retrieve the publish message with given packetId.
     *
     * @param packetId the packetId of the publish message
     * @return publish message stored with given packetId
     */
    MqttPublishMessage retrieve(int packetId);

    /**
     * remove the publish message with given packetId
     *
     * @param packetId the packetId of the publish message to remove
     * @return the removed publish message
     */
    MqttPublishMessage remove(int packetId);

    /**
     * Returns all the message stored in the message store.
     *
     * @return all of the messages stored, or null if no message stored
     */
    Set<MqttPublishMessage> getAllMessages();

    /**
     *
     * @param maxInFlight
     */
    void setMaxInFlight(int maxInFlight);

    /**
     *
     * @param offlineOptions
     */
    void setOfflineOptions(OfflineOptions offlineOptions);

    /**
     *
     * @param message
     */
    void appendToOutboundZone(MqttMessage message);

    /**
     *
     * @param message
     */
    void prependToOutboundZone(MqttMessage message);

    /**
     *
     * @return
     */
    MqttMessage popFromOutboundZone();

    /**
     *
     * @param message
     * @throws MqttException
     */
    void appendToOfflineZone(MqttMessage message) throws MqttException;

    /**
     *
     * @return
     */
    MqttMessage popFromOfflineZone();

    /**
     * @return the next usable packetId by natural order.
     * @throws MqttException if no id is available, an MqttException with reason code <code>REASON_CODE_NO_MESSAGE_IDS_AVAILABLE</code> will be thrown
     */
    int getNextPacketId() throws MqttException;

    /**
     * manually release the packet id, for publish message packet id can be released when in-flight message is acknowledged.
     * but for subscription and unsubscription message packet id should be released when SUBACK and UNSUBACK messages arrived.
     *
     * @param packetId the packet id to release
     */
    void releasePacketId(int packetId);

    /**
     *
     * @param packetId
     * @throws MqttException
     */
    void inFlight(int packetId) throws MqttException;

    /**
     *
     * @param packetId
     */
    boolean inFlightAck(int packetId);

    /**
     *
     * @return
     */
    int getInFlightCount();

    /**
     *
     * @return
     */
    Set<Integer> getInFlightPacketIds();

    /**
     *
     * @param packetId
     */
    void waitingForRec(int packetId);

    /**
     *
     * @param packetId
     * @throws MqttException
     */
    void recReceived(int packetId) throws MqttException;

    /**
     *
     * @return
     */
    int getWaitingRecCount();

    /**
     *
     * @return
     */
    Set<Integer> getWaitingRecPacketIds();
}
