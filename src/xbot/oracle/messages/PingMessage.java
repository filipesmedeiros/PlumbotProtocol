package xbot.oracle.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class PingMessage extends ProtocolMessage {

    public static final short MSG_CODE = 301;

    private UUID mId;

    public PingMessage() {
        super(MSG_CODE);
        this.mId = UUID.randomUUID();
    }

    public PingMessage(UUID mId) {
        super(MSG_CODE);
        this.mId = mId;
    }

    public UUID mId() {
        return mId;
    }

    public static final ISerializer<PingMessage> serializer = new ISerializer<PingMessage>() {

        @Override
        public void serialize(PingMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
        }

        @Override
        public PingMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            return new PingMessage(mId);
        }

        @Override
        public int serializedSize(PingMessage m) {
            return (2 * Long.BYTES);
        }
    };
}
