package com.fayayo.socketstart.ch7.libclink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void setReceiveLinstner(IoArgs.IoArgsEventListener listener);

    boolean receiveAsync(IoArgs ioArgs) throws IOException;
}
