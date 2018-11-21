package network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface MessageRequest {

    InetSocketAddress to();

    ByteBuffer message();

    short type();
}
