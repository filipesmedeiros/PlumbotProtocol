package xbot;

import java.net.InetSocketAddress;

public interface Oracle {

    long getCost(InetSocketAddress peer);
}
