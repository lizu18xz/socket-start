package com.fayayo.socketstart.ch8.libclink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);

    boolean postReceiveAsync() throws IOException;
}
