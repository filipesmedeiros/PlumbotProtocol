package protocol.rework;

import java.net.InetSocketAddress;

public interface XBotSupportEdge extends XBotSupport {

    boolean handleCost(InetSocketAddress peer, long cost);
}
