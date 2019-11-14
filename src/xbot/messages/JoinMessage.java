package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class JoinMessage extends ProtocolMessage {

    public static final short MSG_CODE = 203;

    private UUID mId;

    public JoinMessage() {
        super(MSG_CODE);
        this.mId = UUID.randomUUID();
    }

    public JoinMessage(UUID mId) {
        super(MSG_CODE);
        this.mId = mId;
    }

    public static final ISerializer<JoinMessage> serializer = new ISerializer<JoinMessage>() {

        @Override
        public void serialize(JoinMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
        }

        @Override
        public JoinMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            return new JoinMessage(mId);
        }

        @Override
        public int serializedSize(JoinMessage m) {
            return (2 * Long.BYTES);
        }
    };
}
