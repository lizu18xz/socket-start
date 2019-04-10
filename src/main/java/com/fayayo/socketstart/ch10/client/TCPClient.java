package com.fayayo.socketstart.ch10.client;




import com.fayayo.socketstart.ch10.client.bean.ServerInfo;
import com.fayayo.socketstart.ch10.foo.handle.ConnectorHandler;
import com.fayayo.socketstart.ch10.foo.handle.ConnectorStringPacketChain;
import com.fayayo.socketstart.ch10.libclink.box.StringReceivePacket;
import com.fayayo.socketstart.ch10.libclink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel socketChannel, File cachePath) throws IOException {
        super(socketChannel, cachePath);
        getStringPacketChain().appendLast(new PrintStringPacketChain());
    }

    private class PrintStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            System.out.println(str);
            return true;
        }
    }

    static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
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
