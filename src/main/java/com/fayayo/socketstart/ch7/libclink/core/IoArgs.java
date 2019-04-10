package com.fayayo.socketstart.ch7.libclink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * byteBuffer封装
 */
public class IoArgs {

    private int limit = 256;
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    //读数据bytes  到buffer
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    //写数据到bytes
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
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
        buffer.putInt(total);
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    //监听
    public interface IoArgsEventListener {
        void onStarted(IoArgs args);

        void onCompleted(IoArgs args);
    }


}
