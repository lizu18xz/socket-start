package com.fayayo.socketstart.ch8.libclink.impl.async;

import com.fayayo.socketstart.ch8.libclink.box.StringReceivePacket;
import com.fayayo.socketstart.ch8.libclink.core.*;
import com.fayayo.socketstart.ch8.libclink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dalizu on 2018/11/30.
 * @version v1.0
 * @desc
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher ,IoArgs.IoArgsEventProcessor{

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;

    private final ReceivePacketCallback receivePacketCallback;

    private IoArgs ioArgs = new IoArgs();

    private ReceivePacket<?,?> packetTemp;
    private WritableByteChannel packetChannel;

    private long total;

    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receivePacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.receivePacketCallback = receivePacketCallback;
    }

    @Override
    public void start() {

        registerReceive();
    }

    private void registerReceive() {

        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            CloseAndNotify();
        }

    }

    private void CloseAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void stop() {

    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }

    //真正一条数据完成，然后发送
    private void completePacket(boolean isSuccessed) {
        ReceivePacket packet = this.packetTemp;
        packetTemp=null;
        CloseUtils.close(packet);
        WritableByteChannel channel=this.packetChannel;
        CloseUtils.close(channel);
        packetChannel=null;

        if(packet!=null){
            receivePacketCallback.onReceivePacketCompleted(packet);
        }
    }

    //解析数据到packet,消息大于我们的缓冲区，会分批发送，多次，直到等于我们的消息长度，就获取到了一条完整的消息
    private void assemblePacket(IoArgs args) {

        //如果说明读取的固定头部，此时构造一个 body实际长度的包
        if (packetTemp == null) {
            int length = args.readLength();

            //TODO 暂时通过大小判断是文件还是字符串
            byte type=length>200? Packet.TYPE_STREAM_FILE:Packet.TYPE_MEMORY_STRING;

            packetTemp=receivePacketCallback.onArrivedNewPacket(type,length);

            packetChannel= Channels.newChannel(packetTemp.open());
            total = length;
            position = 0;
        }

        try {
            int count = args.writeTo(packetChannel);//返回当前读取到的长度，把数据写入通道
            position += count;
            if (position == total) {
                completePacket(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            completePacket(false);
        }

    }


    @Override
    public IoArgs provideIoArgs() {
        IoArgs args=ioArgs;
        //设置可以接收数据的大小，先获取固定头部。
        int receiveSize;
        if (packetTemp == null) {
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(total - position, args.capacity());//还剩下多少数据，buffer的大小
        }
        //设置本次接收数据的大小
        args.limit(receiveSize);
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {

        //解析数据到packet
        assemblePacket(args);
        //继续读取
        registerReceive();
    }
}
