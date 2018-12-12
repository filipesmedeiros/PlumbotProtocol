package message.plumtree;

import message.PlumbotMessage;

import java.net.InetSocketAddress;

public abstract class HopMessage extends PlumbotMessage {

    private int hops;

    public HopMessage(InetSocketAddress sender, short type) {
        super(sender, type);
        hops = 0;
    }

    public int hops() {
        return hops;
    }
}
