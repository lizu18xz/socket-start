package com.fayayo.socketstart.ch10.libclink.frames;


import com.fayayo.socketstart.ch10.libclink.core.IoArgs;

/**
 * 心跳接收帧
 */
public class HeartbeatReceiveFrame extends AbsReceiveFrame {
    static final HeartbeatReceiveFrame INSTANCE = new HeartbeatReceiveFrame();

    private HeartbeatReceiveFrame() {
        super(HeartbeatSendFrame.HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IoArgs args) {
        return 0;
    }
}
