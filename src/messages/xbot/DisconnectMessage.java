package messages.xbot;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class DisconnectMessage extends MessageWithSender {

    private boolean wait;

    public DisconnectMessage(InetSocketAddress sender, boolean wait) {
        super(sender);
        this.wait = wait;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public MessageType type() {
        return null;
    }

    @Override
    public ByteBuffer serialize() {
        return null;
    }

    public boolean isWait() {
        return wait;
    }
}
