package network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class GeneralMessageRequest implements MessageRequest {

    private ByteBuffer msg;
    private InetSocketAddress to;

    public GeneralMessageRequest(ByteBuffer msg, InetSocketAddress to) {
        this.msg = msg;
        this.to = to;
    }

    @Override
    public InetSocketAddress to() {
        return to;
    }

    @Override
    public ByteBuffer message() {
        return msg;
    }

    @Override
    public short type() {
        return msg.getShort(0);
    }
}
