package network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Network {

    private static Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    private Network() {
    }

    public void send(ByteBuffer m, InetSocketAddress to) {
        System.out.println(m.position());
    }
}
