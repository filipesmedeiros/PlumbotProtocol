package notifications;

import java.net.InetSocketAddress;

public class TCPConnectionNotification implements Notification {

    public static final short TYPE = 1;

    private InetSocketAddress peer;

    // true if this was an accept, false if this was a connection this node made
    private boolean accept;

    public TCPConnectionNotification(InetSocketAddress peer, boolean accept) {
        this.peer = peer;
        this.accept = accept;
    }

    @Override
    public short type() {
        return TYPE;
    }

    public InetSocketAddress peer() {
        return peer;
    }

    public boolean accept() {
        return accept;
    }
}
