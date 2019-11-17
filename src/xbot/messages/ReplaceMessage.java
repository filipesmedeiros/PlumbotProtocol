package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class ReplaceMessage extends ProtocolMessage {

    public static final short MSG_CODE = 206;

    private UUID mId;
    private Host initiator;
    private Host old;
    private long itoc;
    private long itoo;

    public ReplaceMessage(UUID mId, Host initiator, Host old, long itoc, long itoo) {
        super(MSG_CODE);
        this.mId = mId;
        this.initiator = initiator;
        this.old = old;
        this.itoc = itoc;
        this.itoo = itoo;
    }

    public ReplaceMessage() {
        super(MSG_CODE);
        this.initiator = null;
        this.old = null;
        this.itoc = 0;
        this.itoo = 0;
        this.mId = UUID.randomUUID();
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

    public long itoo() {
        return itoo;
    }

    public ReplaceMessage setInitiator(Host initiator) {
        this.initiator = initiator;
        return this;
    }

    public ReplaceMessage setOld(Host old) {
        this.old = old;
        return this;
    }

    public ReplaceMessage setItoc(long itoc) {
        this.itoc = itoc;
        return this;
    }

    public ReplaceMessage setItoo(long itoo) {
        this.itoo = itoo;
        return this;
    }

    public static final ISerializer<ReplaceMessage> serializer = new ISerializer<ReplaceMessage>() {

        @Override
        public void serialize(ReplaceMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            m.initiator.serialize(out);
            m.old.serialize(out);
            out.writeLong(m.itoc);
            out.writeLong(m.itoo);
        }

        @Override
        public ReplaceMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            Host initiator = Host.deserialize(in);
            Host old = Host.deserialize(in);
            long itoc = in.readLong();
            long itoo = in.readLong();
            return new ReplaceMessage(mId, initiator, old, itoc, itoo);
        }

        @Override
        public int serializedSize(ReplaceMessage m) {
            return (4 * Long.BYTES) + m.initiator.serializedSize() + m.old.serializedSize();
        }
    };
}
