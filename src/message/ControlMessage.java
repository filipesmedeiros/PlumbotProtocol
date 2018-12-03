package message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public abstract class ControlMessage implements Message {

    private InetSocketAddress sender;
    private short type;

    public ControlMessage(InetSocketAddress sender, short type) {
        this.sender = sender;
        this.type = type;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    @Override
    public int size() {
        return 2 + sender().toString().length() * 2 + 2 + 1;
    }

    @Override
    public short messageType() {
        return type;
    }

    // Will not work in the real world(?)
    public static InetSocketAddress parseAddress(ByteBuffer bytes) {
        StringBuilder hostStr = new StringBuilder();

        while(true) {
            char c = bytes.getChar();
            if(c == ':')
                break;
            hostStr.append(c);
        }

        StringBuilder portStr = new StringBuilder();

        while(true) {
            char c = bytes.getChar();
            if(c == Message.EOS)
                break;
            portStr.append(c);
        }

        int port = Integer.parseInt(portStr.toString());

        String host;
        try {
            host = InetAddress.getByName(hostStr.toString().substring(1)).getHostAddress();
        } catch(UnknownHostException e) {
            e.printStackTrace();
            return null;
        }

        return new InetSocketAddress(host, port);
    }

    public ByteBuffer putSenderInBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(size());

        buffer.putShort(type);

        char[] senderChars = sender().toString().toCharArray();
        for(int i = 0; i < senderChars.length; i++) {
            buffer.putChar(senderChars[i]);
        }

        buffer.putChar(EOS);

        return buffer;
    }

    public ByteBuffer putAddressInBuffer(ByteBuffer buffer, InetSocketAddress address) {
        char[] chars = address.toString().toCharArray();
        for(int i = 0; i < chars.length; i++)
            buffer.putChar(chars[i]);

        buffer.putChar(EOS);

        return buffer;
    }
}
