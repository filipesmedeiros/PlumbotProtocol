package nettyFoutoRefactor.network.messaging.serializers;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.Host;
import nettyFoutoRefactor.network.ISerializer;
import nettyFoutoRefactor.network.messaging.messages.AcceptJoinMessage;

import java.net.UnknownHostException;

public class AcceptJoinMesSer implements ISerializer<AcceptJoinMessage> {

    @Override
    public void serialize(AcceptJoinMessage acceptJoinMessage, ByteBuf out) {
        acceptJoinMessage.sender().serialize(out);
        out.writeBoolean(acceptJoinMessage.accepted());
    }

    @Override
    public AcceptJoinMessage deserialize(ByteBuf in) throws UnknownHostException {
        return new AcceptJoinMessage(Host.deserialize(in), in.readBoolean());
    }

    @Override
    public int serializedSize(AcceptJoinMessage acceptJoinMessage) {
        return acceptJoinMessage.sender().serializedSize() + 1; // Booleans take up 1 byte in ByteBuf
    }
}
