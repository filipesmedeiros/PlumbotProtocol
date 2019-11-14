package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class SwitchMessage extends MessageWithSender {

    private InetSocketAddress initiator;
    private InetSocketAddress candidate;
    private long dtoo;

    public SwitchMessage(InetSocketAddress sender, InetSocketAddress initiator, InetSocketAddress candidate, long dtoo) {
        super(sender);
        this.initiator = initiator;
        this.candidate = candidate;
        this.dtoo = dtoo;
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

    public InetSocketAddress initiator() {
        return initiator;
    }

    public InetSocketAddress candidate() {
        return candidate;
    }

    public long dtoo() {
        return dtoo;
    }
}
