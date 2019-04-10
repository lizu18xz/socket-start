package com.fayayo.socketstart.ch8.client;

import com.fayayo.socketstart.ch8.client.bean.ServerInfo;
import com.fayayo.socketstart.ch8.foo.Foo;
import com.fayayo.socketstart.ch8.libclink.box.FileSendPacket;
import com.fayayo.socketstart.ch8.libclink.core.IoContext;
import com.fayayo.socketstart.ch8.libclink.impl.IoSelectorProvider;

import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client");

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())//初始化
                .start();


        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
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
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

            //文件发送
            if (str.startsWith("--f")) {
                String[] arr = str.split(" ");
                if (arr.length >= 2) {
                    String filePath = arr[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }
            }

            // 发送字符串
            tcpClient.send(str);
        } while (true);
    }
}
