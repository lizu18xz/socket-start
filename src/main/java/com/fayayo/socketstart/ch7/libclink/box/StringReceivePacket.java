package com.fayayo.socketstart.ch7.libclink.box;

import com.fayayo.socketstart.ch7.libclink.core.ReceivePacket;

import java.io.IOException;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class StringReceivePacket extends ReceivePacket {

    private byte[] buffer;
    private int position;

    public StringReceivePacket(int len) {

        buffer = new byte[len];
        length = len;

    }

    @Override
    public void save(byte[] bytes, int count) {

        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;

    }

    public String string() {
        return new String(buffer);
    }

    @Override
    public void close() throws IOException {

    }
}
