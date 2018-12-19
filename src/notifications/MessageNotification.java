package notifications;

import java.nio.ByteBuffer;

public class MessageNotification implements Notification {

    public static final short TYPE = 2;

    private ByteBuffer message;

    public MessageNotification(ByteBuffer message) {
        this.message = message;
    }

    @Override
    public short type() {
        return TYPE;
    }

    public short msgType() {
        return message.getShort();
    }

    public ByteBuffer message() {
        return message;
    }
}
