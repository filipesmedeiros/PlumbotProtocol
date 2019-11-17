package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class ReplaceReplyMessage extends ProtocolMessage {

    public static final short MSG_CODE = 207;

    private UUID mId;
    private boolean answer;
    private Host initiator;
    private Host old;
    private long itoc;

    public ReplaceReplyMessage(UUID mId, boolean answer, Host initiator, Host old, long itoc) {
        super(MSG_CODE);
        this.mId = mId;
        this.answer = answer;
        this.initiator = initiator;
        this.old = old;
        this.itoc = itoc;
    }

    public ReplaceReplyMessage() {
        super(MSG_CODE);
        this.answer = false;
        this.initiator = null;
        this.old = null;
        this.itoc = 0;
        this.mId = UUID.randomUUID();
    }

    public boolean answer() {
        return answer;
    }

    public Host initiator() {
        return initiator;
    }

    public Host old() {
        return old;
    }

    public long itoc() {
        return itoc;
    }

    public ReplaceReplyMessage setAnswer(boolean answer) {
        this.answer = answer;
        return this;
    }

    public ReplaceReplyMessage setInitiator(Host initiator) {
        this.initiator = initiator;
        return this;
    }

    public ReplaceReplyMessage setOld(Host old) {
        this.old = old;
        return this;
    }

    public ReplaceReplyMessage setItoc(long itoc) {
        this.itoc = itoc;
        return this;
    }

    public static final ISerializer<ReplaceReplyMessage> serializer = new ISerializer<ReplaceReplyMessage>() {

        @Override
        public void serialize(ReplaceReplyMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeBoolean(m.answer);
            m.initiator.serialize(out);
            m.old.serialize(out);
            out.writeLong(m.itoc);
        }

        @Override
        public ReplaceReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            boolean answer = in.readBoolean();
            Host initiator = Host.deserialize(in);
            Host old = Host.deserialize(in);
            long itoc = in.readLong();
            return new ReplaceReplyMessage(mId, answer, initiator, old, itoc);
        }

        @Override
        public int serializedSize(ReplaceReplyMessage m) {
            return (3 * Long.BYTES) + 1 + m.initiator.serializedSize() + m.old.serializedSize();
        }
    };
}
