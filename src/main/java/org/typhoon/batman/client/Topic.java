/*
 * Copyright (c) 2019. All rights reserved.
 * Topic.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client;

import java.io.Serializable;
import java.util.*;

/**
 * @author C.
 */
public class Topic implements Serializable {

    private final String topic;

    private final transient List<TopicToken> topicTokens;
    private final transient boolean valid;

    public Topic(String topic) {
        this.topic = topic;
        topicTokens = parseTopic(topic);
        valid = topicTokens != null;
    }

    public List<TopicToken> getTopicTokens() {
        return topicTokens;
    }

    private List<TopicToken> parseTopic(String topic) {
        List<TopicToken> res = new ArrayList<>();
        if (topic.equals("/")) {
            res.add(TopicToken.EMPTY);
            res.add(TopicToken.EMPTY);
            return res;
        }
        String[] tokens = topic.split("/");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) {
                res.add(TopicToken.EMPTY);
            } else if (token.equals(TopicToken.MULTI.getValue())) {
                // check that multi is the last symbol
                if (i < token.length() - 1) {
                    return null;
                }
                res.add(TopicToken.MULTI);
            } else if (token.equals(TopicToken.SINGLE.getValue())) {
                res.add(TopicToken.SINGLE);
            } else if (token.contains(TopicToken.MULTI.getValue())) {
                return null;
            } else if (token.contains(TopicToken.SINGLE.getValue())) {
                return null;
            } else {
                res.add(new TopicToken(token));
            }
        }
        if (topic.endsWith("/")) {
            // Add a fictitious space
            res.add(TopicToken.EMPTY);
        }
        return res;
    }

    public boolean isValid() {
        return valid;
    }

    public static Topic asTopic(String s) {
        return new Topic(s);
    }

    /**
     * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
     *
     * @param subscriptionTopic the topic filter of the subscription
     * @return true if the two topics match.
     */
    public boolean match(Topic subscriptionTopic) {
        List<TopicToken> msgTokens = getTopicTokens();
        List<TopicToken> subscriptionTokens = subscriptionTopic.getTopicTokens();

        Iterator<TopicToken> msgTokenIterator = msgTokens.iterator();
        for (TopicToken subToken : subscriptionTokens) {
            if (subToken == TopicToken.MULTI) {
                return true;
            }
            if (subToken != TopicToken.SINGLE) {
                if (!msgTokenIterator.hasNext()) {
                    return false;
                }
                TopicToken msgToken = msgTokenIterator.next();
                if (!msgToken.equals(subToken)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return topic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Topic other = (Topic) obj;
        return Objects.equals(this.topic, other.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }
}
