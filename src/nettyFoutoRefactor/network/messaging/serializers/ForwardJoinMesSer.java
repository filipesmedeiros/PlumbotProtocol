package nettyFoutoRefactor.network.messaging.serializers;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.Host;
import nettyFoutoRefactor.network.ISerializer;
import nettyFoutoRefactor.network.messaging.messages.ForwardJoinMessage;

import java.net.UnknownHostException;

public class ForwardJoinMesSer implements ISerializer<ForwardJoinMessage> {

    @Override
    public void serialize(ForwardJoinMessage forwardJoinMessage, ByteBuf out) {
        forwardJoinMessage.sender().serialize(out);
        out.writeShort(forwardJoinMessage.ttl());
    }

    @Override
    public ForwardJoinMessage deserialize(ByteBuf in) throws UnknownHostException {
        Host sender = Host.deserialize(in);
        short ttl = in.readShort();
        return new ForwardJoinMessage(sender, ttl);
    }

    @Override
    public int serializedSize(ForwardJoinMessage forwardJoinMessage) {
        return forwardJoinMessage.sender().serializedSize() + 2;
    }
}
