package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class OptimizeMessage extends MessageWithSender {

    private InetSocketAddress old;
    private long itoc;
    private long itoo;

    public OptimizeMessage(InetSocketAddress sender, InetSocketAddress old, long itoc, long itoo) {
        super(sender);
        this.old = old;
        this.itoc = itoc;
        this.itoo = itoo;
    }

    @Override
    public int size() {
        return baseSize() + 22; // 6 InetSocketAddress
    }

    @Override
    public MessageType type() {
        return MessageType.Optimize;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
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
