package com.fayayo.socketstart.ch7.libclink.impl.async;

import com.fayayo.socketstart.ch7.libclink.box.StringReceivePacket;
import com.fayayo.socketstart.ch7.libclink.core.IoArgs;
import com.fayayo.socketstart.ch7.libclink.core.ReceiveDispatcher;
import com.fayayo.socketstart.ch7.libclink.core.ReceivePacket;
import com.fayayo.socketstart.ch7.libclink.core.Receiver;
import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dalizu on 2018/11/30.
 * @version v1.0
 * @desc
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;

    private final ReceivePacketCallback receivePacketCallback;

    private IoArgs ioArgs = new IoArgs();

    private ReceivePacket packetTemp;

    private byte[] buffer;

    private int total;

    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receivePacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveLinstner(listener);
        this.receivePacketCallback = receivePacketCallback;
    }

    @Override
    public void start() {

        registerReceive();
    }

    private void registerReceive() {

        try {
            receiver.receiveAsync(ioArgs);
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

            ReceivePacket packet = packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }

        }

    }


    private IoArgs.IoArgsEventListener listener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
            //设置可以接收数据的大小，先获取固定头部。
            int receiveSize;
            if (packetTemp == null) {
                receiveSize = 4;
            } else {
                receiveSize = Math.min(total - position, args.capacity());//还剩下多少数据，buffer的大小
            }
            //设置本次接收数据的大小
            args.limit(receiveSize);
        }

        @Override
        public void onCompleted(IoArgs args) {

            assemblePacket(args);

            //继续接收下一条数据
            registerReceive();

        }
    };

    //真正一条数据完成，然后发送
    private void completePacket() {
        ReceivePacket packet = this.packetTemp;

        CloseUtils.close(packet);
        receivePacketCallback.onReceivePacketCompleted(packet);

    }

    //解析数据到packet,消息大于我们的缓冲区，会分批发送，多次，直到等于我们的消息长度，就获取到了一条完整的消息
    private void assemblePacket(IoArgs args) {

        //如果说明读取的固定头部，此时构造一个 body实际长度的包
        if (packetTemp == null) {
            int length = args.readLength();//取出后buffer中的数据
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }

        int count = args.writeTo(buffer, 0);//写数据到 buffer  取出后buffer中的数据
        if (count > 0) {
            packetTemp.save(buffer, count);
            position += count;
            if (position == total) {
                completePacket();
                packetTemp = null;
            }
        }

    }


}
