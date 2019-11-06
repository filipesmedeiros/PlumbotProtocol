package common;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Node {

    InetSocketAddress id();

    void send(ByteBuffer data, InetSocketAddress to);

    void join(InetSocketAddress joinNode);
}
