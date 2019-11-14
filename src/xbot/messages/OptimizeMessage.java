package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class OptimizeMessage extends ProtocolMessage {

    public static final short MSG_CODE = 204;

    private UUID mId;
    private Host old;
    private long itoc;
    private long itoo;

    public OptimizeMessage(UUID mId, Host old, long itoc, long itoo) {
        super(MSG_CODE);
        this.mId = mId;
        this.old = old;
        this.itoc = itoc;
        this.itoo = itoo;
    }

    public OptimizeMessage() {
        super(MSG_CODE);
        this.old = null;
        this.itoc = 0;
        this.itoo = 0;
        this.mId = UUID.randomUUID();
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

    public void setOld(Host old) {
        this.old = old;
    }

    public void setItoc(long itoc) {
        this.itoc = itoc;
    }

    public void setItoo(long itoo) {
        this.itoo = itoo;
    }

    public static final ISerializer<OptimizeMessage> serializer = new ISerializer<OptimizeMessage>() {

        @Override
        public void serialize(OptimizeMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            m.old.serialize(out);
            out.writeLong(m.itoc);
            out.writeLong(m.itoo);
        }

        @Override
        public OptimizeMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            Host old = Host.deserialize(in);
            long itoc = in.readLong();
            long itoo = in.readLong();
            return new OptimizeMessage(mId, old, itoc, itoo);
        }

        @Override
        public int serializedSize(OptimizeMessage m) {
            return (4 * Long.BYTES) + m.old.serializedSize();
        }
    };
}
