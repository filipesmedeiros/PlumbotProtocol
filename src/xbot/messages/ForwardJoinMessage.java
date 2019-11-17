package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class ForwardJoinMessage extends ProtocolMessage {

    public static final short MSG_CODE = 202;

    private UUID mId;
    private Host joiningPeer;
    private int ttl;

    public ForwardJoinMessage() {
        super(MSG_CODE);
        this.joiningPeer = null;
        this.ttl = 0;
        this.mId = UUID.randomUUID();
    }

    public ForwardJoinMessage(UUID mId, Host joiningPeer, int ttl) {
        super(MSG_CODE);
        this.joiningPeer = joiningPeer;
        this.ttl = ttl;
    }

    public UUID mId() {
        return mId;
    }

    public Host joiningPeer() {
        return joiningPeer;
    }

    public ForwardJoinMessage setJoiningPeer(Host joiningPeer) {
        this.joiningPeer = joiningPeer;
        return this;
    }

    public int ttl() {
        return ttl;
    }

    public ForwardJoinMessage setTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    public static final ISerializer<ForwardJoinMessage> serializer = new ISerializer<ForwardJoinMessage>() {

        @Override
        public void serialize(ForwardJoinMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            m.joiningPeer.serialize(out);
            out.writeInt(m.ttl);
        }

        @Override
        public ForwardJoinMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            Host joiningPeer = Host.deserialize(in);
            int ttl = in.readInt();
            return new ForwardJoinMessage(mId, joiningPeer, ttl);
        }

        @Override
        public int serializedSize(ForwardJoinMessage m) {
            return (2 * Long.BYTES) + m.joiningPeer.serializedSize() + Integer.BYTES;
        }
    };
}
