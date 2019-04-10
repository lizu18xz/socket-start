package com.fayayo.socketstart.ch7.server;


import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;
import com.fayayo.socketstart.ch7.server.handler.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private ClientListener listener;
    //注意多线程问题   集合遍历，添加  删除  会有线程的问题,不能保证遍历 安全
    //private List<ClientHandler> clientHandlerList = Collections.synchronizedList(new ArrayList<>());
    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    private final ExecutorService forwardingThreadPoolExecutor;

    private Selector selector;

    private ServerSocketChannel socketChannel;

    public TCPServer(int port) {
        this.port = port;
        forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector=Selector.open();//获取selector
            ServerSocketChannel socketChannel=ServerSocketChannel.open();//打开通道
            socketChannel.configureBlocking(false);//非阻塞
            //绑定本地端口
            socketChannel.socket().bind(new InetSocketAddress(port));
            //注册客户端到达的监听
            socketChannel.register(selector, SelectionKey.OP_ACCEPT);

            this.socketChannel=socketChannel;

            System.out.println("服务器信息：" + socketChannel.getLocalAddress().toString());
            //启动客户端的监听
            ClientListener listener =this.listener= new ClientListener();//通过一个 线程 初始化 我们的服务器服务
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }

        //关闭资源
        CloseUtils.close(socketChannel);
        CloseUtils.close(selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
        forwardingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        //消息输出
        //System.out.println("Receive-"+handler.getClientInfo()+":"+msg);
        //System.out.println("Receive-"+handler.getClientInfo()+":"+msg.replace("\n","-\\n-"));
        //异步操作
        forwardingThreadPoolExecutor.execute(()->{
            for (ClientHandler clientHandler:clientHandlerList){
                if(clientHandler.equals(handler)){
                    //跳过
                    continue;
                }
                clientHandler.send(msg);
            }
        });

    }

    //单独启动一个线程 来 监听客户端的请求
    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();

            Selector selector=TCPServer.this.selector;
            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                try {
                    if(selector.select()==0){//唤醒状态=0
                        if(done){
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator= selector.selectedKeys().iterator();//遍历所有连接的客户端
                    while (iterator.hasNext()){
                        if(done){
                            break;
                        }

                        SelectionKey key=iterator.next();
                        iterator.remove();//移除掉当前的
                        //检查当前key的状态是否是我们关注的客户端到达事件
                        if(key.isAcceptable()){
                            ServerSocketChannel serverSocketChannel= (ServerSocketChannel) key.channel();//就是我们注册的ServerSocketChannel

                            SocketChannel socketChannel=serverSocketChannel.accept();//此处的accept一定可以直接返回 不会阻塞  ：返回的是客户端

                            try {
                                // 客户端构建异步线程  处理获取的socket
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this );
                                // 添加同步处理
                                synchronized(TCPServer.this){
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            selector.wakeup();//唤醒当前阻塞
        }
    }

}
