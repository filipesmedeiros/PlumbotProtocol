package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.Host;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class OptimizeReplyMessage extends ProtocolMessage {

    public static final short MSG_CODE = 205;

    private UUID mId;
    private Host old;
    private boolean answer;
    private boolean hasDisconnect;
    private Host disconnect;
    private long itoc;

    public OptimizeReplyMessage(UUID mId, Host old, boolean answer, boolean hasDisconnect, Host disconnect,
                                long itoc) {
        super(MSG_CODE);
        this.mId = mId;
        this.old = old;
        this.answer = answer;
        this.hasDisconnect = hasDisconnect;
        this.disconnect = disconnect;
        this.itoc = itoc;
    }

    public OptimizeReplyMessage() {
        super(MSG_CODE);
        this.old = null;
        this.answer = false;
        this.hasDisconnect = false;
        this.disconnect = null;
        this.itoc = 0;
        this.mId = UUID.randomUUID();
    }

    public boolean answer() {
        return answer;
    }

    public Host old() {
        return old;
    }

    public boolean hasDisconnect() {
        return hasDisconnect;
    }

    public Host disconnect() {
        return disconnect;
    }

    public long itoc() {
        return itoc;
    }

    public OptimizeReplyMessage setOld(Host old) {
        this.old = old;
        return this;
    }

    public OptimizeReplyMessage setAnswer(boolean answer) {
        this.answer = answer;
        return this;
    }

    public OptimizeReplyMessage setHasDisconnect(boolean hasDisconnect) {
        this.hasDisconnect = hasDisconnect;
        return this;
    }

    public OptimizeReplyMessage setDisconnect(Host disconnect) {
        this.disconnect = disconnect;
        return this;
    }

    public OptimizeReplyMessage setItoc(long itoc) {
        this.itoc = itoc;
        return this;
    }

    public static final ISerializer<OptimizeReplyMessage> serializer = new ISerializer<OptimizeReplyMessage>() {

        @Override
        public void serialize(OptimizeReplyMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            m.old.serialize(out);
            out.writeBoolean(m.answer);
            out.writeBoolean(m.hasDisconnect);
            m.disconnect.serialize(out);
            out.writeLong(m.itoc);
        }

        @Override
        public OptimizeReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            Host old = Host.deserialize(in);
            boolean answer = in.readBoolean();
            boolean hasDisconnect = in.readBoolean();
            Host disconnect = Host.deserialize(in);
            long itoc = in.readLong();
            return new OptimizeReplyMessage(mId, old, answer, hasDisconnect, disconnect, itoc);
        }

        @Override
        public int serializedSize(OptimizeReplyMessage m) {
            return (3 * Long.BYTES) + m.old.serializedSize() + 2;
        }
    };
}
