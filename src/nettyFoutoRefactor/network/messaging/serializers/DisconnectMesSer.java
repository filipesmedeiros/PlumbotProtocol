package nettyFoutoRefactor.network.messaging.serializers;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.Host;
import nettyFoutoRefactor.network.ISerializer;
import nettyFoutoRefactor.network.messaging.messages.DisconnectMessage;

import java.net.UnknownHostException;

public class DisconnectMesSer implements ISerializer<DisconnectMessage> {

    @Override
    public void serialize(DisconnectMessage disconnectMessage, ByteBuf out) {
        disconnectMessage.sender().serialize(out);
    }

    @Override
    public DisconnectMessage deserialize(ByteBuf in) throws UnknownHostException {
        return new DisconnectMessage(Host.deserialize(in));
    }

    @Override
    public int serializedSize(DisconnectMessage disconnectMessage) {
        return 0;
    }
}
