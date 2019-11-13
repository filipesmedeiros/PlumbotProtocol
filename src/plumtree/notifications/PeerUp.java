package plumtree.notifications;

import babel.notification.ProtocolNotification;
import network.Host;

public class PeerUp extends ProtocolNotification {

    public static final short NOTIFICATION_CODE = 103;
    public static final String NOTIFICATION_NAME = "Peer Up";

    private Host peer;

    public PeerUp(Host peer) {
        super(NOTIFICATION_CODE, NOTIFICATION_NAME);
        this.peer = peer;
    }

    public Host peer() {
        return peer;
    }
}
