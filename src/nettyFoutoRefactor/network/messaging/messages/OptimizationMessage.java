package nettyFoutoRefactor.network.messaging.messages;

import nettyFoutoRefactor.network.Host;

public class OptimizationMessage extends MessageWithSender {

    private final Host old;
    private final long itoo;
    private final long itoc;

    public OptimizationMessage(Host sender, Host old, long itoo, long itoc) {
        super(sender);
        this.old = old;
        this.itoo = itoo;
        this.itoc = itoc;
    }

    public Host old() {
        return old;
    }

    public long itoo() {
        return itoo;
    }

    public long itoc() {
        return itoc;
    }
}
