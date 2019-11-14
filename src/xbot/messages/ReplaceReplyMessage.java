package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class ReplaceReplyMessage extends MessageWithSender {

    private boolean answer;
    private InetSocketAddress initiator;
    private InetSocketAddress old;

    public ReplaceReplyMessage(InetSocketAddress sender, boolean answer, InetSocketAddress initiator,
                               InetSocketAddress old) {
        super(sender);
        this.answer = answer;
        this.initiator = initiator;
        this.old = old;
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

    public InetSocketAddress initiator() {
        return initiator;
    }

    public InetSocketAddress old() {
        return old;
    }
}
