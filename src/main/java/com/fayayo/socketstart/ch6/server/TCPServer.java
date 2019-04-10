package com.fayayo.socketstart.ch6.server;


import com.fayayo.socketstart.ch6.server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private ClientListener mListener;
    //注意多线程问题   集合遍历，添加  删除  会有线程的问题,不能保证遍历 安全
    //private List<ClientHandler> clientHandlerList = Collections.synchronizedList(new ArrayList<>());
    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port);//通过一个 线程 初始化 我们的服务器服务
            mListener = listener;
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }

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
        System.out.println("Receive-"+handler.getClientInfo()+":"+msg);
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
        private ServerSocket server;
        private boolean done = false;

        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + " P:" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                Socket client;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    continue;
                }
                try {
                    // 客户端构建异步线程  处理获取的socket
                    ClientHandler clientHandler = new ClientHandler(client,TCPServer.this );
                    // 读取数据并打印
                    clientHandler.readToPrint();
                    synchronized(TCPServer.this){
                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常：" + e.getMessage());
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
