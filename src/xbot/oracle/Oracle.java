package xbot.oracle;

import messages.Message;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

public interface Oracle {

    Future<Long> getCost(InetSocketAddress peer);

    void deliverMessage(Message m);
}
