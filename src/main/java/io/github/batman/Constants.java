/*
 * Copyright (c) 2019. All rights reserved.
 * Constants.java created at 2019-10-08 11:33:13
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package io.github.batman;

/**
 * @author C.
 */
public final class Constants {

    public static final char[] TOPIC_WILDCARDS = {'#', '+'};
    public static final int MAX_PAYLOAD_LENGTH = 268435455;
    public static final int MIN_PACKET_ID = 1;
    public static final int MAX_PACKET_ID = 65535;
    public static final int MIN_CLIENT_ID_LENGTH_V3_1 = 1;
    public static final int MAX_CLIENT_ID_LENGTH_V3_1 = 23;
    public static final int MAX_CLIENT_ID_LENGTH_V3_1_1 = 65535;
    public static final int MAX_TOPIC_LENGTH_V3_1 = 32767;
    public static final int MAX_TOPIC_LENGTH_V_3_1_1 = 65535;

    private Constants() {

    }
}
