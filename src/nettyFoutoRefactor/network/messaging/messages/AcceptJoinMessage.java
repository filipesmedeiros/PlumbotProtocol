package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class AcceptJoinMessage extends MessageWithSender {

    private boolean accepted;

    public AcceptJoinMessage(Host sender, boolean accepted) {
        super(sender);
        this.accepted = accepted;
    }

    public boolean accepted() {
        return accepted;
    }
}
