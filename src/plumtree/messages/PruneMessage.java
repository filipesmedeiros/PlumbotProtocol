package plumtree.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class PruneMessage extends ProtocolMessage {

    public static final short MSG_CODE = 104;

    private final UUID mId;

    public PruneMessage() {
        super(MSG_CODE);
        this.mId = UUID.randomUUID();
    }

    public PruneMessage(UUID mId) {
        super(MSG_CODE);
        this.mId = mId;
    }

    public static final ISerializer<PruneMessage> serializer = new ISerializer<PruneMessage>() {

        @Override
        public void serialize(PruneMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
        }

        @Override
        public PruneMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            return new PruneMessage(mId);
        }

        @Override
        public int serializedSize(PruneMessage m) {
            return (2 * Long.BYTES);
        }
    };
}
