package com.fayayo.socketstart.ch8.libclink.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 接收包
 */
public abstract class ReceivePacket<Stream extends OutputStream,Entity> extends Packet<Stream> {

    private Entity entity;

    public ReceivePacket(long len) {
        this.length=len;
    }


    public Entity entity(){
        return entity;
    }

    /**
     * 根据接收到的流转化为对应的实体
     *
     * @param stream {@link OutputStream}
     * @return 实体
     */
    protected abstract Entity buildEntity(Stream stream);


    @Override
    protected final void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        //创建entity
        entity=buildEntity(stream);
    }
}
