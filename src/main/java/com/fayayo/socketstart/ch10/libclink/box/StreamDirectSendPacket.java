package com.fayayo.socketstart.ch10.libclink.box;


import com.fayayo.socketstart.ch10.libclink.core.Packet;
import com.fayayo.socketstart.ch10.libclink.core.SendPacket;

import java.io.InputStream;

/**
 * 直流发送Packet
 */
public class StreamDirectSendPacket extends SendPacket<InputStream> {
    private InputStream inputStream;

    public StreamDirectSendPacket(InputStream inputStream) {
        // 用以读取数据进行输出的输入流
        this.inputStream = inputStream;
        // 长度不固定，所以为最大值
        this.length = MAX_PACKET_SIZE;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }

    @Override
    protected InputStream createStream() {
        return inputStream;
    }
}
