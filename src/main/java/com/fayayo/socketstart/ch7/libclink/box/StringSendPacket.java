package com.fayayo.socketstart.ch7.libclink.box;

import com.fayayo.socketstart.ch7.libclink.core.SendPacket;

import java.io.IOException;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class StringSendPacket extends SendPacket {

    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public void close() throws IOException {

    }
}
