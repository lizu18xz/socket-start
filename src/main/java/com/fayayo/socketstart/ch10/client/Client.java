package com.fayayo.socketstart.ch10.client;




import com.fayayo.socketstart.ch10.client.bean.ServerInfo;
import com.fayayo.socketstart.ch10.foo.Foo;
import com.fayayo.socketstart.ch10.foo.handle.ConnectorCloseChain;
import com.fayayo.socketstart.ch10.foo.handle.ConnectorHandler;
import com.fayayo.socketstart.ch10.libclink.box.FileSendPacket;
import com.fayayo.socketstart.ch10.libclink.core.Connector;
import com.fayayo.socketstart.ch10.libclink.core.IoContext;
import com.fayayo.socketstart.ch10.libclink.core.ScheduleJob;
import com.fayayo.socketstart.ch10.libclink.core.schedule.IdleTimeoutScheduleJob;
import com.fayayo.socketstart.ch10.libclink.impl.IoSelectorProvider;
import com.fayayo.socketstart.ch10.libclink.impl.SchedulerImpl;
import com.fayayo.socketstart.ch10.libclink.utils.CloseUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
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

                // 添加关闭操作
                tcpClient.getCloseChain().appendLast(new ConnectorCloseChain() {
                    @Override
                    protected boolean consume(ConnectorHandler handler, Connector connector) {
                        CloseUtils.close(System.in);
                        return true;
                    }
                });

                // 开始调度一份心跳包
                ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.SECONDS, tcpClient);
                tcpClient.schedule(scheduleJob);

                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
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
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }

            if (str.length() == 0) {
                continue;
            }

            // --f url
            if (str.startsWith("--f")) {
                String[] array = str.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
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
