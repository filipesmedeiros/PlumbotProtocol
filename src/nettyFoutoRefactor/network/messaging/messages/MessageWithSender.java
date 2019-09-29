package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class MessageWithSender {

    private final Host sender;

    MessageWithSender(Host sender) {
        this.sender = sender;
    }

    public Host sender() {
        return sender;
    }
}
