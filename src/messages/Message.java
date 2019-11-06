package messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Message {

    enum MessageType {
        Join,
        Data;

        public short code() {
            return (short) this.hashCode();
        }
    }

    InetSocketAddress sender();

    long id();

    MessageType type();

    ByteBuffer serialize();
}
