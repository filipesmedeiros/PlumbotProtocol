package network;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface TCPInterface extends NetworkInterface {

    void connect(InetSocketAddress remote) throws IOException;

    void disconnect(InetSocketAddress remote) throws IOException;
}
