package xbot;

import common.Node;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class XBotNode implements Node {

    private InetSocketAddress id;

    public XBotNode(InetSocketAddress id) {
        this.id = id;
    }


    @Override
    public InetSocketAddress id() {
        return id;
    }

    @Override
    public void send(ByteBuffer data, InetSocketAddress to) {

    }

    @Override
    public void join(InetSocketAddress joinNode) {

    }
}
