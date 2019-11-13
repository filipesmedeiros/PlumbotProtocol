package plumtree.notifications;

import babel.notification.ProtocolNotification;

public class BroadcastDeliver extends ProtocolNotification {

    public static final short NOTIFICATION_CODE = 101;
    public static final String NOTIFICATION_NAME = "Broadcast Deliver";

    private byte[] message;

    public BroadcastDeliver(byte[] message) {
        super(NOTIFICATION_CODE, NOTIFICATION_NAME);
        if(message != null) {
            this.message = new byte[message.length];
            System.arraycopy(message, 0, this.message, 0, message.length);
        } else
            this.message = new byte[0];
    }

    public byte[] message() {
        return message;
    }
}
