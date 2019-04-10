package com.fayayo.socketstart.ch8.libclink.box;

import com.fayayo.socketstart.ch8.libclink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class StringSendPacket extends BytesSendPacket{

    /**
     * 字符串发送时就是Byte数组，所以直接得到Byte数组，并按照Byte的发送方式发送即可
     *
     * @param msg 字符串
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

}
