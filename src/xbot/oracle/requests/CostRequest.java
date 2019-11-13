package xbot.oracle.requests;

import babel.requestreply.ProtocolRequest;
import network.Host;

public class CostRequest extends ProtocolRequest {

    public static final short REQUEST_CODE = 301;

    private Host peer;

    public CostRequest(Host peer) {
        super(REQUEST_CODE);
        this.peer = peer;
    }

    public Host peer() {
        return peer;
    }
}
