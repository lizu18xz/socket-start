package com.fayayo.socketstart.ch7.server;

import com.fayayo.socketstart.ch7.libclink.core.IoContext;
import com.fayayo.socketstart.ch7.libclink.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.fayayo.socketstart.ch6.foo.constants.TCPConstants.PORT_SERVER;

public class Server {
    public static void main(String[] args) throws IOException {

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())//初始化
                .start();

        TCPServer tcpServer = new TCPServer(PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
            return;
        }

        //UDP相关
        UDPProvider.start(PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            tcpServer.broadcast(str);//广播
        } while (!"00bye00".equalsIgnoreCase(str));

        UDPProvider.stop();
        tcpServer.stop();
        IoContext.close();
    }
}
