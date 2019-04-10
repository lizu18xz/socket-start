package com.fayayo.socketstart.ch8.server;

import com.fayayo.socketstart.ch8.foo.Foo;
import com.fayayo.socketstart.ch8.libclink.core.IoContext;
import com.fayayo.socketstart.ch8.libclink.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.fayayo.socketstart.ch6.foo.constants.TCPConstants.PORT_SERVER;

public class Server {
    public static void main(String[] args) throws IOException {


        File cachePath= Foo.getCacheDir("server");

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())//初始化
                .start();

        TCPServer tcpServer = new TCPServer(PORT_SERVER,cachePath);
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
            if("00bye00".equalsIgnoreCase(str)){
                break;
            }
            //发生字符串
            tcpServer.broadcast(str);
        } while (true);
        UDPProvider.stop();
        tcpServer.stop();
        IoContext.close();
    }
}
