package com.fayayo.socketstart.ch10.libclink.impl;


import com.fayayo.socketstart.ch10.libclink.core.IoArgs;
import com.fayayo.socketstart.ch10.libclink.core.IoProvider;
import com.fayayo.socketstart.ch10.libclink.core.Receiver;
import com.fayayo.socketstart.ch10.libclink.core.Sender;
import com.fayayo.socketstart.ch10.libclink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Cloneable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    // 最后活跃时间点
    private volatile long lastReadTime = System.currentTimeMillis();
    private volatile long lastWriteTime = System.currentTimeMillis();

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        receiveIoEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        // 进行Callback状态监测，判断是否处于自循环状态
        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        // 进行Callback状态监测，判断是否处于自循环状态
        outputCallback.checkAttachNull();
        // 当前发送的数据附加到回调中
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            // 关闭
            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    private final IoProvider.HandleProviderCallback inputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }

            // 刷新读取时间
            lastReadTime = System.currentTimeMillis();

            final IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            if (processor == null) {
                return;
            }

            if (args == null) {
                args = processor.provideIoArgs();
            }

            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        // 本次回调就代表可以进行数据消费，
                        // 但是如果一个数据也没有产生消费，那么我们尝试输出一句语句到控制台
                        System.out.println("Current read zero data!");
                    }

                    // 检查是否还有空闲区间，以及是否需要填满空闲区间
                    if (args.remained() && args.isNeedConsumeRemaining()) {
                        // 附加当前未消费完成的args
                        attach = args;
                        // 再次注册数据发送
                        ioProvider.registerInput(channel, this);
                    } else {
                        // 设置为null
                        attach = null;
                        // 读取完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };


    private final IoProvider.HandleProviderCallback outputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }

            // 刷新输出时间
            lastWriteTime = System.currentTimeMillis();

            final IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            if (processor == null) {
                return;
            }

            if (args == null) {
                // 拿一份新的IoArgs
                args = processor.provideIoArgs();
            }

            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else {
                    int count = args.writeTo(channel);
                    if (count == 0) {
                        // 本次回调就代表可以进行数据消费，
                        // 但是如果一个数据也没有产生消费，那么我们尝试输出一句语句到控制台
                        System.out.println("Current write zero data!");
                    }

                    // 检查是否还有未消费数据，以及是否需要一次消费完全
                    if (args.remained() && args.isNeedConsumeRemaining()) {
                        // 附加当前未消费完成的args
                        attach = args;
                        // 再次注册数据发送
                        ioProvider.registerOutput(channel, this);
                    } else {
                        // 设置为null
                        attach = null;
                        // 输出完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
