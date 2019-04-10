package com.fayayo.socketstart.ch8.client;


import com.fayayo.socketstart.ch8.client.bean.ServerInfo;
import com.fayayo.socketstart.ch8.foo.Foo;
import com.fayayo.socketstart.ch8.libclink.core.Connector;
import com.fayayo.socketstart.ch8.libclink.core.Packet;
import com.fayayo.socketstart.ch8.libclink.core.ReceivePacket;
import com.fayayo.socketstart.ch8.libclink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {

    private final File cachePath;

    public TCPClient(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        setup(socketChannel);
    }


    public void exit() {
        CloseUtils.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已关闭，数据无法发送");
    }

    @Override
    protected File createNewReceiveFile() {

        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket receivePacket) {
        super.onReceivedPacket(receivePacket);
        if (receivePacket.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) receivePacket.entity();
            System.out.println(key.toString() + ":" + string);
        }
    }

    public static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());

        try {

            return new TCPClient(socketChannel, cachePath);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }
        return null;
    }

}
