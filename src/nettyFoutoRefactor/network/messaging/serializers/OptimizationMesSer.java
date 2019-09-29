package nettyFoutoRefactor.network.messaging.serializers;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.Host;
import nettyFoutoRefactor.network.ISerializer;
import nettyFoutoRefactor.network.messaging.messages.OptimizationMessage;

import java.net.UnknownHostException;

public class OptimizationMesSer implements ISerializer<OptimizationMessage> {

    @Override
    public void serialize(OptimizationMessage optimizationMessage, ByteBuf out) {
        optimizationMessage.sender().serialize(out);
        optimizationMessage.old().serialize(out);
        out.writeLong(optimizationMessage.itoo());
        out.writeLong(optimizationMessage.itoc());
    }

    @Override
    public OptimizationMessage deserialize(ByteBuf in) throws UnknownHostException {
        Host sender = Host.deserialize(in);
        Host old = Host.deserialize(in);
        long itoo = in.readLong();
        long itoc = in.readLong();
        return new OptimizationMessage(sender, old, itoo, itoc);
    }

    @Override
    public int serializedSize(OptimizationMessage optimizationMessage) {
        return 0;
    }
}
