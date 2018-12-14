package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public interface PersistantNetwork extends NetworkInterface {

    SocketChannel connect(InetSocketAddress remote) throws IOException;

    boolean disconnect(InetSocketAddress remote) throws IOException;
}
