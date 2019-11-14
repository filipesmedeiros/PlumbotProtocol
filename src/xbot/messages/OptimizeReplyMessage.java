package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class OptimizeReplyMessage extends MessageWithSender {

    private InetSocketAddress old;
    private boolean answer;
    private boolean hasDisconnect;
    private InetSocketAddress disconnect;

    public OptimizeReplyMessage(InetSocketAddress sender, InetSocketAddress old, boolean answer, boolean hasDisconnect,
                                InetSocketAddress disconnect) {
        super(sender);
        this.old = old;
        this.answer = answer;
        this.hasDisconnect = hasDisconnect;
        this.disconnect = disconnect;
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

    public boolean answer() {
        return answer;
    }

    public InetSocketAddress old() {
        return old;
    }

    public boolean hasDisconnect() {
        return hasDisconnect;
    }

    public InetSocketAddress disconnect() {
        return disconnect;
    }
}
