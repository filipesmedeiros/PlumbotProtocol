package network;

import common.concurrent.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class TCP {

    private Selector selector;
    private Map<InetSocketAddress, SocketChannel> channels;

    private ServerSocketChannel serverChannel;

    public TCP(short listenPort) throws IOException {
        channels = new HashMap<>();

        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(listenPort));

        selector = Selector.open();

        serverChannel.register(selector, serverChannel.validOps());
    }

    public void init() {
        ThreadPool.getInstance().submit(() -> {
                    try {
                        this.listen();
                    } catch(IOException ioe) {
                        ioe.printStackTrace();

                        // TODO
                    }
                }
        );
    }

    private void listen() throws IOException {
        while(true) {
            selector.select();
            for(SelectionKey key : selector.selectedKeys()) {
                if(key.channel().equals(serverChannel) && key.isAcceptable()) {
                    SocketChannel newPeer = ((ServerSocketChannel) key.channel()).accept();
                    channels.put(newPeer.finishConnect(), newPeer);
                }
            }
        }
    }
}
