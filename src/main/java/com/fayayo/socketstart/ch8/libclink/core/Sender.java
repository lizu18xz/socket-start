package com.fayayo.socketstart.ch8.libclink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    void setSenderListener(IoArgs.IoArgsEventProcessor processor);

    boolean postSendAsync() throws IOException;
}
