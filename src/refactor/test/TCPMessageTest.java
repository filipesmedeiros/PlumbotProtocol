package refactor.test;

import refactor.message.Message;
import refactor.message.MessageDecoder;
import refactor.network.TCP;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TCPMessageTest {

    public static void main(String[] args) throws IOException {
        InetSocketAddress local = new InetSocketAddress(3000);

        InetSocketAddress remote = new InetSocketAddress(4000);

        TCP localTcp = new TCP(local);
        TCP remoteTcp = new TCP(remote);

        Message message1 = new Message(MessageDecoder.MessageType.content);
        localTcp.connect(remote);
    }
}
