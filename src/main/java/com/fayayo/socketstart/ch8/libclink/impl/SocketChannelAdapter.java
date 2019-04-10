package com.fayayo.socketstart.ch8.libclink.impl;

import com.fayayo.socketstart.ch8.libclink.core.IoArgs;
import com.fayayo.socketstart.ch8.libclink.core.IoProvider;
import com.fayayo.socketstart.ch8.libclink.core.Receiver;
import com.fayayo.socketstart.ch8.libclink.core.Sender;
import com.fayayo.socketstart.ch8.libclink.utils.CloseUtils;

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

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;

    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;


    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangeListener listener)
            throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);

    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {

        receiveIoEventProcessor=processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        //接收
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerInput(channel, inputCallback);//注册完毕 会唤醒read操作继续执行
    }

    @Override
    public void setSenderListener(IoArgs.IoArgsEventProcessor processor) {

        sendIoEventProcessor=processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        // 当前发送的数据附加到回调中
        // 回调当前Channel已关闭
        //注册当前的socketChannel到selector.
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


    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }

            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;

            IoArgs args = processor.provideIoArgs();

            try {
                // 具体的读取操作，从socketChannel读取数据到buffer
                if (args.readFrom(channel) > 0) {
                    // 读取完成回调
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args,new IOException("Cannot readFrom any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };


    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput() {
            if (isClosed.get()) {
                return;
            }

            IoArgs.IoArgsEventProcessor processor=sendIoEventProcessor;
            IoArgs args = processor.provideIoArgs();

            try {
                // 具体的读取操作:通过channel按块写出，真正的写出！！！
                if (args.writeTo(channel) > 0) {
                    // 读取完成回调
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args,new IOException("Cannot write any data!"));
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
