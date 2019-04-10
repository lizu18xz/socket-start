package com.fayayo.socketstart.ch7.libclink.core;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 发送包定义
 */
public abstract class SendPacket extends Packet{

    private boolean isCanceled;

    public abstract byte[] bytes();

    public boolean isCanceled(){
        return isCanceled;
    }



}
