package xbot.requests;

import babel.requestreply.ProtocolReply;
import network.Host;

import java.util.List;
import java.util.UUID;

public class GetPeersReply extends ProtocolReply {

    public static final short REPLY_CODE = GetPeersRequest.REQUEST_CODE;

    private final UUID requestId;
    private final List<Host> peers;

    public GetPeersReply(UUID requestId, List<Host> peers) {
        super(REPLY_CODE);
        this.requestId = requestId;
        this.peers = peers;
    }

    public UUID requestId() {
        return requestId;
    }

    public List<Host> peers() {
        return peers;
    }
}
