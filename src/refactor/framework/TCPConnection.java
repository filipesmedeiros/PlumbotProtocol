package refactor.framework;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class TCPConnection {

    private SocketChannel channel;

    public TCPConnection(InetSocketAddress remote) {
        try {
            channel = SocketChannel.open();
        } catch(IOException ioe) {
            // TODO
            ioe.printStackTrace();
            System.exit(1);
        }
        try {
            channel.bind(null);
        } catch(IOException uhe) {
            // TODO

        }
    }

    public InetSocketAddress remote()
            throws IOException {
        SocketAddress remote = channel.getRemoteAddress();
        if(!(remote instanceof InetSocketAddress))
            throw new IOException();
        return (InetSocketAddress) remote;
    }

    public InetSocketAddress local()
            throws IOException {
        SocketAddress local = channel.getLocalAddress();
        if(!(local instanceof InetSocketAddress))
            throw new IOException();
        return (InetSocketAddress) local;
    }

    public void close()
            throws IOException {
        channel.close();
    }
}
