package plumtree.requests;

import babel.requestreply.ProtocolRequest;

public class BroadcastRequest extends ProtocolRequest {

    public static final short REQUEST_CODE = 101;

    private byte[] payload;

    public BroadcastRequest(byte[] message) {
        super(BroadcastRequest.REQUEST_CODE);

        if(message != null) {
            this.payload = new byte[message.length];
            System.arraycopy(message, 0, this.payload, 0, message.length);
        } else
            this.payload = new byte[0];
    }

    public byte[] payload() {
        return payload;
    }
}