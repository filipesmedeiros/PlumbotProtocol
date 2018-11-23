package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SwitchMessage extends ControlMessage {

    public static final short TYPE = 11;

    private InetSocketAddress init;

    public SwitchMessage(InetSocketAddress sender, InetSocketAddress init) {
        super(sender, TYPE);
        this.init = init;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();
        putAddressInBuffer(buffer, init);

        buffer.put(EOT).flip();

        return buffer;
    }

    @Override
    public int size() {
        return super.size() + init.toString().length() * 2 + 2
                + 1;
    }

    public InetSocketAddress init() {
        return init;
    }

    public static SwitchMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);
        InetSocketAddress init = parseAddress(bytes);

        return new SwitchMessage(sender, init);
    }
}
