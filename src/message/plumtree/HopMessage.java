package message.plumtree;

import message.PlumbotMessage;

import java.net.InetSocketAddress;

public abstract class HopMessage extends PlumbotMessage {

    private short hops;

    HopMessage(InetSocketAddress sender, short type, short hops) {
        super(sender, type);
        this.hops = hops;
    }

    short hops() {
        return hops;
    }

    void nextHop() {
        hops++;
    }
}
