package refactor.message;

import java.net.InetSocketAddress;

// TODO Javadoc this class
public class MessageWithSender extends Message {

    private InetSocketAddress sender;

    public MessageWithSender(MessageDecoder.MessageType messageType, InetSocketAddress sender) {
        super(messageType);
        this.sender = sender;
    }

    public InetSocketAddress sender() {
        return sender;
    }

    public MessageWithSender setSender(InetSocketAddress sender) {
        this.sender = sender;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof MessageWithSender))
            return false;
        if(!super.equals(other))
            return false;
        return ((MessageWithSender) other).sender.equals(sender);
    }
}
