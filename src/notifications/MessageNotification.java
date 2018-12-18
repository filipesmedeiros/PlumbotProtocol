package notifications;

import java.nio.ByteBuffer;

public class MessageNotification implements Notification {

    public static final short TYPE = 2;

    private int senderPort;

    private ByteBuffer message;

    public MessageNotification(ByteBuffer message, int senderPort) {
        this.senderPort = senderPort;

        this.message = message;
    }

    @Override
    public short type() {
        return TYPE;
    }

    public short msgType() {
        return message.getShort();
    }

    public int senderPort() {
        return senderPort;
    }

    public ByteBuffer message() {
        return message;
    }
}
