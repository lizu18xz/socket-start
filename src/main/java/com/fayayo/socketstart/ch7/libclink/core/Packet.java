package com.fayayo.socketstart.ch7.libclink.core;

import java.io.Closeable;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 公共数据封装，提供类型以及最基本的长度定义
 */
public abstract class Packet implements Closeable{

    protected byte type;

    protected int length;

    public byte type(){
        return type;
    }

    public int length(){
        return length;
    }


}
