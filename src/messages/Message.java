package messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public interface Message {

    enum MessageType {
        Join,
        ForwardJoin,
        Optimize,
        OptimizeReply,
        Replace,
        ReplaceReply,
        Switch,
        SwitchReply,
        Disconnect,
        IHave,
        Prune,
        Graft,
        Broadcast,
        Ping;

        public short code() {
            return (short) this.hashCode();
        }
    }

    InetSocketAddress sender();

    UUID id();

    MessageType type();

    ByteBuffer serialize();
}
