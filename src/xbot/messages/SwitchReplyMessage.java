package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class SwitchReplyMessage extends ProtocolMessage {

    public static final short MSG_CODE = 209;

    private UUID mId;
    private boolean answer;
    private Host initiator;
    private Host candidate;

    public SwitchReplyMessage(UUID mId, boolean answer, Host initiator, Host candidate) {
        super(MSG_CODE);
        this.mId = mId;
        this.answer = answer;
        this.initiator = initiator;
        this.candidate = candidate;
    }

    public SwitchReplyMessage() {
        super(MSG_CODE);
        this.answer = false;
        this.initiator = null;
        this.candidate = null;
        this.mId = UUID.randomUUID();
    }

    public boolean answer() {
        return answer;
    }

    public Host initiator() {
        return initiator;
    }

    public Host candidate() {
        return candidate;
    }

    public SwitchReplyMessage setAnswer(boolean answer) {
        this.answer = answer;
        return this;
    }

    public SwitchReplyMessage setInitiator(Host initiator) {
        this.initiator = initiator;
        return this;
    }

    public SwitchReplyMessage setCandidate(Host candidate) {
        this.candidate = candidate;
        return this;
    }

    public static final ISerializer<SwitchReplyMessage> serializer = new ISerializer<SwitchReplyMessage>() {

        @Override
        public void serialize(SwitchReplyMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeBoolean(m.answer);
            m.initiator.serialize(out);
            m.candidate.serialize(out);
        }

        @Override
        public SwitchReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            boolean answer = in.readBoolean();
            Host initiator = Host.deserialize(in);
            Host candidate = Host.deserialize(in);
            return new SwitchReplyMessage(mId, answer, initiator, candidate);
        }

        @Override
        public int serializedSize(SwitchReplyMessage m) {
            return (2 * Long.BYTES) + 1 + m.initiator.serializedSize() + m.candidate.serializedSize();
        }
    };
}
