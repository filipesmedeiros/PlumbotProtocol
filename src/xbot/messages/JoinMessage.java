package xbot.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;

public class JoinMessage extends MessageWithSender {

    public static final MessageType TYPE = MessageType.Join;

    public JoinMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    @Override
    public int size() {
        return BYTE_SIZE + 2;
    }
}
