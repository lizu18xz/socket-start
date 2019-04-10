package com.fayayo.socketstart.ch8.libclink.box;

import com.fayayo.socketstart.ch8.libclink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author dalizu on 2018/12/31.
 * @version v1.0
 * @desc
 */
public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {

    private File file;

    public FileReceivePacket(long len,File file) {
        super(len);
        this.file=file;
    }

    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
