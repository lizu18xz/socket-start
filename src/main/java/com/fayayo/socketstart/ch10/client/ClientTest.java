package com.fayayo.socketstart.ch10.client;



import com.fayayo.socketstart.ch10.client.bean.ServerInfo;
import com.fayayo.socketstart.ch10.foo.Foo;
import com.fayayo.socketstart.ch10.libclink.core.IoContext;
import com.fayayo.socketstart.ch10.libclink.impl.IoSelectorProvider;
import com.fayayo.socketstart.ch10.libclink.impl.SchedulerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static boolean done;

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);
        if (info == null) {
            return;
        }

        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClients = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }

                tcpClients.add(tcpClient);

                System.out.println("连接成功：" + (++size));

            } catch (IOException | NullPointerException e) {
                System.out.println("连接异常");
                break;
            }
        }


        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                for (TCPClient tcpClient : tcpClients) {
                    tcpClient.send("Hello~~");
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        System.in.read();

        // 等待线程完成
        done = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 客户端结束操作
        for (TCPClient tcpClient : tcpClients) {
            tcpClient.exit();
        }

        IoContext.close();
    }


}
