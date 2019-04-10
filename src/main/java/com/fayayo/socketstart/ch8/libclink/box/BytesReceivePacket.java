package com.fayayo.socketstart.ch8.libclink.box;

import java.io.ByteArrayOutputStream;

import static com.fayayo.socketstart.ch8.libclink.core.Packet.TYPE_MEMORY_BYTES;

/**
 * 纯Byte数组接收包
 */
public class BytesReceivePacket extends AbsByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(long len) {
        super(len);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }
}