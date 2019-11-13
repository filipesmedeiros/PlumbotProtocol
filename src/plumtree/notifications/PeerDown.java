package plumtree.notifications;

import babel.notification.ProtocolNotification;
import network.Host;

public class PeerDown extends ProtocolNotification {

    public static final short NOTIFICATION_CODE = 102;
    public static final String NOTIFICATION_NAME = "Peer Down";

    private Host peer;

    public PeerDown(Host peer) {
        super(NOTIFICATION_CODE, NOTIFICATION_NAME);
        this.peer = peer;
    }

    public Host peer() {
        return peer;
    }
}
