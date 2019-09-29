package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class ForwardJoinMessage extends MessageWithSender {

    private final short ttl;

    public ForwardJoinMessage(Host sender, short ttl) {
        super(sender);
        this.ttl = ttl;
    }

    public short ttl() {
        return ttl;
    }
}
