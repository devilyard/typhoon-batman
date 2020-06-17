/*
 * Copyright (c) 2019. All rights reserved.
 * PacketIdSerializer.java created at 2019-10-08 11:33:12
 * This file is for internal use only, it is belong to TYPHOON,
 * you cannot redistribute it nor modify it for any purpose.
 */
package org.typhoon.batman.client.store;

/**
 * @author C.
 */
public class PacketIdSerializer {

    public static byte[] serialize(int packetId) {
        return new byte[]{(byte) (packetId >>> 8), (byte) (packetId & 0xff)};
    }

    public static int deserialize(byte[] bytes) {
        return ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff);
    }
}
