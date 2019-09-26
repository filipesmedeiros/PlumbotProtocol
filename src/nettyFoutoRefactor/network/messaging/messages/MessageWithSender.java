package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class MessageWithSender {

    private Host sender;

    public MessageWithSender(Host sender) {
        this.sender = sender;
    }

    public Host sender() {
        return sender;
    }
}
