package com.fayayo.socketstart.ch8.libclink.box;

import com.fayayo.socketstart.ch8.libclink.core.SendPacket;

import java.io.*;

/**
 * @author dalizu on 2018/11/29.
 * @version v1.0
 * @desc
 */
public class FileSendPacket extends SendPacket<FileInputStream> {

    private final File file;

    public FileSendPacket(File file) {
        this.file=file;
        this.length = file.length();
    }


    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 使用File构建文件读取流，用以读取本地的文件数据进行发送
     *
     * @return 文件读取流
     */
    @Override
    protected FileInputStream createStream() {

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
