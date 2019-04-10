package com.fayayo.socketstart.ch8.libclink.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * byteBuffer封装
 */
public class IoArgs {

    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(limit);

    //从channel  读取数据到buffer
    public int readFrom(ReadableByteChannel channel) throws IOException {
        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);//从channel读取数据到buffer
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        finishWriting();
        return bytesProduced;
    }

    //写数据到bytes
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);//写到buffer--> 丢给  channel，通过channel按块写出
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        return bytesProduced;
    }

    //从socketChannel读取数据
    public int readFrom(SocketChannel channel) throws IOException {

        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);//从channel读取数据到buffer
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        finishWriting();

        return bytesProduced;
    }

    //先读取到buffer 通过socketChannel再发送出去
    public int writeTo(SocketChannel channel) throws IOException {

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);//写到buffer--> 丢给  channel，通过channel按块写出
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        return bytesProduced;
    }


    //开始写入数据
    public void startWriting() {
        buffer.clear();
        buffer.limit(limit);//定义容纳区间
    }

    //写完后
    public void finishWriting() {

        buffer.flip();
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public void writeLength(int total) {
        startWriting();
        buffer.putInt(total);
        finishWriting();
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    //提供者、处理者，数据的生产或消费者
    public interface IoArgsEventProcessor {

        IoArgs provideIoArgs();//提供一份可消费的IoArgs

        void onConsumeFailed(IoArgs args,Exception e);

        void onConsumeCompleted(IoArgs args);
    }

}
