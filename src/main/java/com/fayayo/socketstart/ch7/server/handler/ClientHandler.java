package com.fayayo.socketstart.ch7.server.handler;



import com.fayayo.socketstart.ch7.libclink.core.Connector;
import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;

import java.io.*;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector{

    private final ClientHandlerCallBack clientHandlerCallBack;

    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo=socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);

        //客户端连接成功后，进行初始化
        setup(socketChannel);
    }

    @Override
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        clientHandlerCallBack.onNewMessageArrived(this,str);
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
