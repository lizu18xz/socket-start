package com.fayayo.socketstart.ch8.libclink.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 发送包定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {


    private boolean isCanceled;

    public boolean isCanceled(){
        return isCanceled;
    }

    /**
     * 设置取消发送标记
     */
    public void cancel() {
        isCanceled = true;
    }

}
