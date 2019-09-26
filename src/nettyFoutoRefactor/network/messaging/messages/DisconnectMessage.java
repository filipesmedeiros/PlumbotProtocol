package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class DisconnectMessage extends MessageWithSender {

    public DisconnectMessage(Host sender) {
        super(sender);
    }
}
