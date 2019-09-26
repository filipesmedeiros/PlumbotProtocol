package nettyFoutoRefactor.network.messaging.serializers;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.Host;
import nettyFoutoRefactor.network.ISerializer;
import nettyFoutoRefactor.network.messaging.messages.JoinMessage;

import java.net.UnknownHostException;

public class JoinMesSer implements ISerializer<JoinMessage> {

    @Override
    public void serialize(JoinMessage joinMessage, ByteBuf out) {
        joinMessage.sender().serialize(out);
    }

    @Override
    public JoinMessage deserialize(ByteBuf in) throws UnknownHostException {
        return new JoinMessage(Host.deserialize(in));
    }

    @Override
    public int serializedSize(JoinMessage joinMessage) {
        return joinMessage.sender().serializedSize();
    }
}
