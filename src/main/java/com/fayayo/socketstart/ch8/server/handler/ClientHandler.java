package com.fayayo.socketstart.ch8.server.handler;


import com.fayayo.socketstart.ch8.foo.Foo;
import com.fayayo.socketstart.ch8.libclink.core.Connector;
import com.fayayo.socketstart.ch8.libclink.core.Packet;
import com.fayayo.socketstart.ch8.libclink.core.ReceivePacket;
import com.fayayo.socketstart.ch8.libclink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector {

    private final File cachePath;

    private final ClientHandlerCallBack clientHandlerCallBack;

    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack,File cachePath) throws IOException {
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo=socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
        this.cachePath=cachePath;
        //客户端连接成功后，进行初始化
        setup(socketChannel);
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
            clientHandlerCallBack.onNewMessageArrived(this,string);
        }
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }


    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出：" +clientInfo);
    }


    private void exitBySelf() {
        exit();
        clientHandlerCallBack.onSelfClosed(this);
    }

    public interface ClientHandlerCallBack {
        //自身关闭的通知
        void onSelfClosed(ClientHandler handler);

        //消息通知出去
        void onNewMessageArrived(ClientHandler handler, String msg);

    }


}
