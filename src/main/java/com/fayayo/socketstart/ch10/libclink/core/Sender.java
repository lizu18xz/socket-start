package com.fayayo.socketstart.ch10.libclink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    boolean postSendAsync() throws IOException;

    /**
     * 获取输出数据的时间
     *
     * @return 毫秒
     */
    long getLastWriteTime();
}
