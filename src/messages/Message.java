package messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public interface Message {

    enum MessageType {
        Join,
        IHave,
        Prune,
        Graft,
        Broadcast;

        public short code() {
            return (short) this.hashCode();
        }
    }

    InetSocketAddress sender();

    UUID id();

    MessageType type();

    ByteBuffer serialize();
}
