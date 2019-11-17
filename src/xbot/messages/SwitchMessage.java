package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class SwitchMessage extends ProtocolMessage {

    public static final short MSG_CODE = 208;

    private UUID mId;
    private Host initiator;
    private Host candidate;
    private long dtoo;

    public SwitchMessage(UUID mId, Host initiator, Host candidate, long dtoo) {
        super(MSG_CODE);
        this.mId = mId;
        this.initiator = initiator;
        this.candidate = candidate;
        this.dtoo = dtoo;
    }

    public SwitchMessage() {
        super(MSG_CODE);
        this.initiator = null;
        this.candidate = null;
        this.dtoo = 0;
        this.mId = UUID.randomUUID();
    }

    public Host initiator() {
        return initiator;
    }

    public Host candidate() {
        return candidate;
    }

    public long dtoo() {
        return dtoo;
    }

    public SwitchMessage setInitiator(Host initiator) {
        this.initiator = initiator;
        return this;
    }

    public SwitchMessage setCandidate(Host candidate) {
        this.candidate = candidate;
        return this;
    }

    public SwitchMessage setDtoo(long dtoo) {
        this.dtoo = dtoo;
        return this;
    }

    public static final ISerializer<SwitchMessage> serializer = new ISerializer<SwitchMessage>() {

        @Override
        public void serialize(SwitchMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            m.initiator.serialize(out);
            m.candidate.serialize(out);
            out.writeLong(m.dtoo);
        }

        @Override
        public SwitchMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            Host initiator = Host.deserialize(in);
            Host candidate = Host.deserialize(in);
            long dtoo = in.readLong();
            return new SwitchMessage(mId, initiator, candidate, dtoo);
        }

        @Override
        public int serializedSize(SwitchMessage m) {
            return (3 * Long.BYTES) + m.initiator.serializedSize() + m.candidate.serializedSize();
        }
    };
}
