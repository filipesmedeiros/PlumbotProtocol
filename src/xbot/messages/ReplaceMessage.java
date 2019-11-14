package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ReplaceMessage extends MessageWithSender {

    private InetSocketAddress initiator;
    private InetSocketAddress old;
    private long itoc;
    private long itoo;

    public ReplaceMessage(InetSocketAddress sender, InetSocketAddress initiator, InetSocketAddress old,
                          long itoc, long itoo) {
        super(sender);
        this.initiator = initiator;
        this.old = old;
        this.itoc = itoc;
        this.itoo = itoo;
    }

    @Override
    public int size() {
        return baseSize() + 28;
    }

    @Override
    public MessageType type() {
        return MessageType.Replace;
    }

    @Override
    public ByteBuffer serialize() {
        return null;
    }

    public InetSocketAddress initiator() {
        return initiator;
    }

    public InetSocketAddress old() {
        return old;
    }

    public long itoc() {
        return itoc;
    }

    public long itoo() {
        return itoo;
    }
}
