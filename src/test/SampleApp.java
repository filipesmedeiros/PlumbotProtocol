package test;

import common.BroadcastListener;
import plumbot.Plumbot;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SampleApp implements BroadcastListener {

    private Plumbot plumbot;

    public SampleApp(InetSocketAddress contactPeer) {
        plumbot = new Plumbot(new InetSocketAddress(0), this);
        plumbot.join(contactPeer);
    }

    public static void main(String[] args) {
        new SampleApp(new InetSocketAddress(args[0], Integer.parseInt(args[1])));
    }

    @Override
    public void deliver(ByteBuffer m) {
        System.out.println(m.position());
    }
}
