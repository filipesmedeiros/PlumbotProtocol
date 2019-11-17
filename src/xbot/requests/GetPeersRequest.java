package xbot.requests;

import babel.requestreply.ProtocolRequest;

import java.util.UUID;

public class GetPeersRequest extends ProtocolRequest {

    public static final short REQUEST_CODE = 201;

    private UUID id;

    public GetPeersRequest() {
        super(REQUEST_CODE);
        this.id = UUID.randomUUID();
    }

    public GetPeersRequest(UUID id) {
        super(REQUEST_CODE);
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
