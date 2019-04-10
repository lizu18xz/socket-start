package com.fayayo.socketstart.ch7.libclink.core;

import java.io.Closeable;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 发送者的调度,缓存所有需要发送的数据
 * 通过队列对数据进行真实的发送，并且在发送时候实现对数据的基本包装
 */
public interface SendDispatcher extends Closeable{

    void send(SendPacket packet);


    void cancel(SendPacket packet);

}
