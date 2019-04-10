package com.fayayo.socketstart.ch7.client;

import com.fayayo.socketstart.ch7.client.bean.ServerInfo;
import com.fayayo.socketstart.ch7.libclink.core.IoContext;
import com.fayayo.socketstart.ch7.libclink.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) throws IOException {

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())//初始化
                .start();


        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient=null;
            try {
                tcpClient= TCPClient.startWith(info);
                if(tcpClient==null){
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                tcpClient.exit();
            }
        }

        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            tcpClient.send(str);
           /* tcpClient.send(str);
            tcpClient.send(str);
            tcpClient.send(str);*/
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }
}
