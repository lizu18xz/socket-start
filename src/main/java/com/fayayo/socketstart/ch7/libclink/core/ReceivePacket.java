package com.fayayo.socketstart.ch7.libclink.core;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 接收包
 */
public abstract class ReceivePacket extends Packet{


    public abstract void save(byte[]bytes,int count);



}
