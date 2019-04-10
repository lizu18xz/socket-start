package com.fayayo.socketstart.ch7.libclink.impl.async;

import com.fayayo.socketstart.ch7.libclink.core.IoArgs;
import com.fayayo.socketstart.ch7.libclink.core.SendDispatcher;
import com.fayayo.socketstart.ch7.libclink.core.SendPacket;
import com.fayayo.socketstart.ch7.libclink.core.Sender;
import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class AsyncSendDispatcher implements SendDispatcher {

    private final Sender sender;

    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();

    private final AtomicBoolean isSending = new AtomicBoolean();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IoArgs ioArgs = new IoArgs();

    private SendPacket packetTemp;

    //当前大小和进度
    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {

        //一条全部发送完毕才能继续下一条，因此加入到队列缓冲
        queue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }

    }


    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            //已取消不用发送
            return takePacket();//递归获取
        }
        return packet;
    }


    private void sendNextPacket() {

        SendPacket temp = packetTemp;
        if (temp != null) {//当前这条还不为空
            CloseUtils.close(temp);
        }

        SendPacket packet = takePacket();//获取一条数据
        packetTemp = packet;
        //发送
        if (packet == null) {
            //队列已经空了
            isSending.set(false);
            return;
        }

        total = packet.length();//有可能比ioArgs大
        position = 0;

        sendCurrentPacket();

    }

    //发送数据
    private void sendCurrentPacket() {

        IoArgs args = ioArgs;//数据装入到IoArgs

        args.startWriting();
        if (position >= total) {//说明数据全发送完毕,获取新数据发送，否则根据上次的postition继续发送上次留下的消息
            sendNextPacket();//发送下一条消息
            return;
        } else if (position == 0) {
            //首包,需要携带长度信息,如果是第一次发送这条数据，就加上4字节的长度,用于固定长度
            args.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();//实际内容
        int count = args.readFrom(bytes, position);//写入到IoArgs 中的 buffer
        position += count;
        //完成封装
        args.finishWriting();

        try {
            sender.sendAsync(args, ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    private void closeAndNotify() {

        CloseUtils.close(this);

    }

    @Override
    public void cancel(SendPacket packet) {

    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            //继续发送当前包
            sendCurrentPacket();
        }
    };

    @Override
    public void close() throws IOException {

        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);

            SendPacket packet = this.packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }
}
