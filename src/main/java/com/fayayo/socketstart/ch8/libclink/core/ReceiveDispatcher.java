package com.fayayo.socketstart.ch8.libclink.core;

import java.io.Closeable;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 接收的调度
 * 实现一份或者多份IOARGS组成一个Packet
 */
public interface ReceiveDispatcher extends Closeable{

    void start();

    void stop();

    interface ReceivePacketCallback{

        ReceivePacket<?,?>onArrivedNewPacket(byte type,long length);

        void onReceivePacketCompleted(ReceivePacket packet);
    }

}
