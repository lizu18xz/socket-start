package com.fayayo.socketstart.ch8.libclink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc 公共数据封装，提供类型以及最基本的长度定义
 */
public abstract class Packet<Stream extends Closeable> implements Closeable{

    // BYTES 类型
    public static final byte TYPE_MEMORY_BYTES = 1;
    // String 类型
    public static final byte TYPE_MEMORY_STRING = 2;
    // 文件 类型
    public static final byte TYPE_STREAM_FILE = 3;
    // 长链接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;

    private Stream stream;

    protected long length;


    public long length(){
        return length;
    }


    public final Stream open(){
        if(stream==null){
            stream=createStream();
        }
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if(stream!=null){
            closeStream(stream);
            stream=null;
        }
    }


    /**
     * 类型，直接通过方法得到:
     * <p>
     * {@link #TYPE_MEMORY_BYTES}
     * {@link #TYPE_MEMORY_STRING}
     * {@link #TYPE_STREAM_FILE}
     * {@link #TYPE_STREAM_DIRECT}
     *
     * @return 类型
     */
    public abstract byte type();

    protected abstract Stream createStream();

    /**
     * 关闭流，当前方法会调用流的关闭操作
     *
     * @param stream 待关闭的流
     * @throws IOException IO异常
     */
    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    /**
     * 头部额外信息，用于携带额外的校验信息等
     *
     * @return byte 数组，最大255长度
     */
    public byte[] headerInfo() {
        return null;
    }
}
