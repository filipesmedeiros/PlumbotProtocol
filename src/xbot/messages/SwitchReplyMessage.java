package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class SwitchReplyMessage extends MessageWithSender {

    private boolean answer;
    private InetSocketAddress initiator;
    private InetSocketAddress candidate;

    public SwitchReplyMessage(InetSocketAddress sender, boolean answer, InetSocketAddress initiator,
                              InetSocketAddress candidate) {
        super(sender);
        this.answer = answer;
        this.initiator = initiator;
        this.candidate = candidate;
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

    public InetSocketAddress candidate() {
        return candidate;
    }
}
