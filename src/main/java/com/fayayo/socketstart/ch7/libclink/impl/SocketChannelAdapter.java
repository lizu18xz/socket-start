package com.fayayo.socketstart.ch7.libclink.impl;

import com.fayayo.socketstart.ch7.libclink.core.IoArgs;
import com.fayayo.socketstart.ch7.libclink.core.IoProvider;
import com.fayayo.socketstart.ch7.libclink.core.Receiver;
import com.fayayo.socketstart.ch7.libclink.core.Sender;
import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dalizu on 2018/11/26.
 * @version v1.0
 * @desc 发送与接收
 */
public class SocketChannelAdapter implements Sender, Receiver, Cloneable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final SocketChannel channel;

    private final IoProvider ioProvider;

    private final OnChannelStatusChangeListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;

    private IoArgs.IoArgsEventListener sendIoEventListener;

    private IoArgs receiveArgsTemp;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangeListener listener)
            throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);

    }

    @Override
    public void setReceiveLinstner(IoArgs.IoArgsEventListener listener) {

        receiveIoEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IoArgs ioArgs) throws IOException {

        //接收
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        receiveArgsTemp = ioArgs;
        return ioProvider.registerInput(channel, inputCallback);//注册完毕 会唤醒read操作继续执行
    }


    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {

        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        sendIoEventListener = listener;
        // 当前发送的数据附加到回调中
        outputCallback.setAttach(args);
        // 回调当前Channel已关闭
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {

        if (isClosed.compareAndSet(false, true)) {

            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            //关闭
            CloseUtils.close(channel);
            //回调
            listener.onChannelClosed(channel);

        }

    }


    /**
     * 处理read事件
     *
     * 1-获取固定的头部,默认4个字节,设置到IOArgs
     * 2-通过IOArgs从channel中读取数据到到IOArgs
     * 3-根据获取的固定长度得到实际需要接收数据的大小，构造接收的包，并且记录总大小，当前位置
     * 4-继续获取数据的时候会根据剩余未读取的大小和IOArgs容量取小的值  进行设置IOArgs
     * 5-然后通过IOArgs从channel中读取数据到到IOArgs
     * 6-然后将数据写入到真正的对象，判断长度是否完成一个完整的消息
     * */
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = receiveArgsTemp;
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;

            listener.onStarted(args);

            try {
                // 具体的读取操作，从socketChannel读取数据到buffer
                if (args.readFrom(channel) > 0) {
                    // 读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot readFrom any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }


        }
    };


    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {

            if (isClosed.get()) {
                return;
            }

            IoArgs args = getAttach();

            IoArgs.IoArgsEventListener listener = sendIoEventListener;

            listener.onStarted(args);

            try {
                // 具体的读取操作
                if (args.writeTo(channel) > 0) {//通过channel按块写出，真正的写出
                    // 读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot write any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }


        }
    };


    public interface OnChannelStatusChangeListener {

        void onChannelClosed(SocketChannel channel);
    }


}
