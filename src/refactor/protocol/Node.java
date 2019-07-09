package refactor.protocol;

import refactor.utils.MessageListener;

import java.net.InetSocketAddress;

public interface Node extends MessageListener {

    void join(InetSocketAddress contactNode);
}
