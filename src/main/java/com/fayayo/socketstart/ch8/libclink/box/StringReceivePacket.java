package com.fayayo.socketstart.ch8.libclink.box;


import java.io.ByteArrayOutputStream;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long len) {
        super(len);
    }


    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String (stream.toByteArray());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
