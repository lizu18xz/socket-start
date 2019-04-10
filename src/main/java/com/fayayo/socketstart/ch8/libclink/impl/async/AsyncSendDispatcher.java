package com.fayayo.socketstart.ch8.libclink.impl.async;

import com.fayayo.socketstart.ch8.libclink.core.IoArgs;
import com.fayayo.socketstart.ch8.libclink.core.SendDispatcher;
import com.fayayo.socketstart.ch8.libclink.core.SendPacket;
import com.fayayo.socketstart.ch8.libclink.core.Sender;
import com.fayayo.socketstart.ch8.libclink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class AsyncSendDispatcher implements SendDispatcher,IoArgs.IoArgsEventProcessor {

    private final Sender sender;

    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();

    private final AtomicBoolean isSending = new AtomicBoolean();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IoArgs ioArgs = new IoArgs();

    private SendPacket<?> packetTemp;

    //当前大小和进度
    private ReadableByteChannel packetChannel;
    private long total;
    private long position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSenderListener(this);
    }

    @Override
    public void send(SendPacket packet) {

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

        //获取发送包的大小
        total = packet.length();
        position = 0;

        //发送当前获取到的包
        sendCurrentPacket();

    }

    //发送数据
    private void sendCurrentPacket() {

        //判断当前获取到的packet数据是否发送完毕,如果完毕则从队列获取下一条数据，否则根据上次的postition继续发送上次留下的消息
        if (position >= total) {
            completePacket(position == total);
            sendNextPacket();//发送下一条消息
            return;
        }

        //发送获取到的packet
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    private void completePacket(boolean isSucceed){

        SendPacket packet=packetTemp;
        if(packet==null){
            return;
        }
        CloseUtils.close(packet);
        CloseUtils.close(packetChannel);

        packetTemp=null;
        packetChannel=null;
        total=0;
        position=0;
    }

    private void closeAndNotify() {

        CloseUtils.close(this);

    }

    @Override
    public void cancel(SendPacket packet) {

    }

    @Override
    public void close() throws IOException {

        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            //异常关闭导致的完成操作
            completePacket(false);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        //数据装入到IoArgs
        IoArgs args = ioArgs;
        if(packetChannel==null){
            //  打开(createStream)获取到的发送包赋值给 packetChannel
            packetChannel= Channels.newChannel(packetTemp.open());
            //首包,发送长度
            args.limit(4);
            args.writeLength((int)packetTemp.length());
        }else {
            //设置本次可以读取多少
            args.limit((int) Math.min(args.capacity(),total-position));
            try {
                int count=args.readFrom(packetChannel);
                position+=count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        //继续发送当前的包
        sendCurrentPacket();
    }
}
