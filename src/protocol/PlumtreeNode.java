package protocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class PlumtreeNode implements TreeBroadcastNode {



    @Override
    public void broadcast(ByteBuffer buffer) throws IllegalArgumentException {

    }

    @Override
    public int setEagerSize(int size) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public int setLazySize(int size) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void neighbourUp(InetSocketAddress peer) {

    }

    @Override
    public void neighbourDown(InetSocketAddress peer) {

    }

    @Override
    public InetSocketAddress id() {
        return null;
    }
}
