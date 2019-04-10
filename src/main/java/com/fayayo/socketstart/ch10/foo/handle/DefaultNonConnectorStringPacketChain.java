package com.fayayo.socketstart.ch10.foo.handle;


import com.fayayo.socketstart.ch10.libclink.box.StringReceivePacket;

/**
 * 默认String接收节点，不做任何事情
 */
class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
        return false;
    }
}
