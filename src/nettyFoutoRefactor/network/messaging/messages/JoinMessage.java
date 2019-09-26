package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class JoinMessage extends MessageWithSender {

    public JoinMessage(Host sender) {
        super(sender);
    }
}
