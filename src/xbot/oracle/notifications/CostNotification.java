package xbot.oracle.notifications;

import babel.notification.ProtocolNotification;

public class CostNotification extends ProtocolNotification {

    public static final short NOTIFICATION_CODE = 301;
    public static final String NOTIFICATION_NAME = "Cost Notification";

    private long cost;

    public CostNotification(long cost) {
        super(NOTIFICATION_CODE, NOTIFICATION_NAME);
        this.cost = cost;
    }

    public long cost() {
        return cost;
    }
}
