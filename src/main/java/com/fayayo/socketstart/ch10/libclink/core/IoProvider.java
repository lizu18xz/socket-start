package com.fayayo.socketstart.ch10.libclink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    boolean registerInput(SocketChannel channel, HandleProviderCallback callback);

    boolean registerOutput(SocketChannel channel, HandleProviderCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);


    abstract class HandleProviderCallback implements Runnable {
        /**
         * 附加本次未完全消费完成的IoArgs，然后进行自循环
         */
        protected volatile IoArgs attach;

        @Override
        public final void run() {
            onProviderIo(attach);
        }

        /**
         * 可以进行接收或者发送时的回调
         *
         * @param args 携带之前的附加值
         */
        protected abstract void onProviderIo(IoArgs args);

        /**
         * 检查当前的附加值是否未null，如果处于自循环时当前附加值不为null，
         * 此时如果外层有调度注册异步发送或者接收是错误的
         */
        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("Current attach is not empty!");
            }
        }
    }

}
