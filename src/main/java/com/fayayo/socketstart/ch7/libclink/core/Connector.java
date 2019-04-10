package com.fayayo.socketstart.ch7.libclink.core;

import com.fayayo.socketstart.ch7.libclink.box.StringReceivePacket;
import com.fayayo.socketstart.ch7.libclink.box.StringSendPacket;
import com.fayayo.socketstart.ch7.libclink.impl.SocketChannelAdapter;
import com.fayayo.socketstart.ch7.libclink.impl.async.AsyncReceiveDispatcher;
import com.fayayo.socketstart.ch7.libclink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangeListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    //对SocketChannel进行封装
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;

    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);

        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        //启动接收
        receiveDispatcher.start();
    }


    public void send(String msg) {

        SendPacket sendPacket = new StringSendPacket(msg);

        sendDispatcher.send(sendPacket);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }


    protected void onReceiveNewMessage(String str) {
        System.out.println(key.toString() + ":" + str);
    }


    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {

            if (packet instanceof StringReceivePacket) {
                String msg = ((StringReceivePacket) packet).string();
                onReceiveNewMessage(msg);
            }
        }
    };

}
