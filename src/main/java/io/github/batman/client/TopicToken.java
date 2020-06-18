/*
 * Copyright (c) 2019. All rights reserved.
 * TopicToken.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman.client;

/**
 * @author C.
 */
public class TopicToken {

    public static final TopicToken EMPTY = new TopicToken("");
    public static final TopicToken MULTI = new TopicToken("#");
    public static final TopicToken SINGLE = new TopicToken("+");

    private final String value;

    protected TopicToken(String s) {
        value = s;
    }

    protected String getValue() {
        return value;
    }

    public boolean match(TopicToken t) {
        return t != MULTI && t != SINGLE && (this == MULTI || this == SINGLE || equals(t));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TopicToken other = (TopicToken) obj;
        return (this.value == null) ? (other.value == null) : this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
