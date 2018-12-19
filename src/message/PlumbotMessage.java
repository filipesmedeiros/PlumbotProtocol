package message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class PlumbotMessage implements Message {

    protected InetSocketAddress sender;
    private short type;

    public PlumbotMessage(InetSocketAddress sender, short type) {
        this.sender = sender;
        this.type = type;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    @Override
    public int size() {
        return Message.MSG_TYPE_SIZE + sender().toString().length() * 2 + 2 + 1;
    }

    @Override
    public short messageType() {
        return type;
    }

    public ByteBuffer putSenderInBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(size());

        buffer.putShort(type);

        char[] senderChars = sender().toString().toCharArray();
        for(char c : senderChars)
            buffer.putChar(c);

        buffer.putChar(EOS);

        return buffer;
    }

    protected void putAddressInBuffer(ByteBuffer buffer, InetSocketAddress address) {
        char[] chars = address.toString().toCharArray();
        for(char c : chars)
            buffer.putChar(c);

        buffer.putChar(EOS);
    }
}
