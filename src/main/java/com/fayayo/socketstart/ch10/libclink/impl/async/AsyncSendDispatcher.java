package com.fayayo.socketstart.ch10.libclink.impl.async;

import com.fayayo.socketstart.ch10.libclink.core.*;
import com.fayayo.socketstart.ch10.libclink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher,
        IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    /**
     * 发送Packet
     * 首先添加到队列，如果当前状态为未启动发送状态
     * 则，尝试让reader提取一份packet进行数据发送
     * <p>
     * 如果提取数据后reader有数据，则进行异步输出注册
     *
     * @param packet 数据
     */
    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        requestSend();
    }

    /**
     * 发送心跳帧，将心跳帧放到帧发送队列进行发送
     */
    @Override
    public void sendHeartbeat() {
        if (queue.size() > 0) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            requestSend();
        }
    }

    /**
     * 取消Packet操作
     * 如果还在队列中，代表Packet未进行发送，则直接标志取消，并返回即可
     * 如果未在队列中，则让reader尝试扫描当前发送序列，查询是否当前Packet正在发送
     * 如果是则进行取消相关操作
     *
     * @param packet 数据
     */
    @Override
    public void cancel(SendPacket packet) {
        boolean ret = queue.remove(packet);
        if (ret) {
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    /**
     * reader从当前队列中提取一份Packet
     *
     * @return 如果队列有可用于发送的数据则返回该Packet
     */
    @Override
    public SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet == null) {
            return null;
        }

        if (packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }

            // 返回True代表当前有数据需要发送
            if (reader.requestTakePacket()) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    /**
     * 请求网络发送异常时触发，进行关闭操作
     */
    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    /**
     * 关闭操作，关闭自己同时需要关闭reader
     */
    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // reader关闭
            reader.close();
            // 清理队列
            queue.clear();
            // 设置当前发送状态
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    /**
     * 网络发送就绪回调，当前已进入发送就绪状态，等待填充数据进行发送
     * 此时从reader中填充数据，并进行后续网络发送
     *
     * @return NULL，可能填充异常，或者想要取消本次发送
     */
    @Override
    public IoArgs provideIoArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    /**
     * 网络发送IoArgs出现异常
     *
     * @param args IoArgs
     * @param e    异常信息
     */
    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
        // 设置当前发送状态
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }

    /**
     * 网络发送IoArgs完成回调
     * 在该方法进行reader对当前队列的Packet提取，并进行后续的数据发送注册
     *
     * @param args IoArgs
     */
    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 设置当前发送状态
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }
}
