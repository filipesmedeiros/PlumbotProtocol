package xbot.oracle.notifications;

import babel.notification.ProtocolNotification;
import network.Host;

public class CostNotification extends ProtocolNotification {

    public static final short NOTIFICATION_CODE = 301;
    public static final String NOTIFICATION_NAME = "Cost Notification";

    private Host peer;
    private long cost;

    public CostNotification(Host peer, long cost) {
        super(NOTIFICATION_CODE, NOTIFICATION_NAME);
        this.peer = peer;
        this.cost = cost;
    }

    public long cost() {
        return cost;
    }

    public Host peer() {
        return peer;
    }
}
