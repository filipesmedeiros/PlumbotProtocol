package notifications;

import java.net.InetSocketAddress;

public class TCPConnectionNotification implements Notification {

    public static final short TYPE = 1;

    private InetSocketAddress peer;

    public TCPConnectionNotification(InetSocketAddress peer) {
        this.peer = peer;
    }

    @Override
    public short type() {
        return TYPE;
    }

    public InetSocketAddress peer() {
        return peer;
    }
}
