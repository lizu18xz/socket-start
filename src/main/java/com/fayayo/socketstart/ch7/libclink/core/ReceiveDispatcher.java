package com.fayayo.socketstart.ch7.libclink.core;

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
        void onReceivePacketCompleted(ReceivePacket packet);
    }

}
