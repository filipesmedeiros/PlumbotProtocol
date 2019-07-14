package refactor.framework;

import java.nio.ByteBuffer;

public class TCPProtocol extends AbstractProtocol implements TransportProtocol {



    @Override
    public void sendBytes(ByteBuffer byteBuffer) {

    }

    @Override
    public void receiveBytes(ByteBuffer byteBuffer) {

    }

    @Override
    public boolean checkEvent(Event event) {
        if(event instanceof MessageEvent)
            queueEvent(event);
        event.passedBy(this);
        return true;
    }

    @Override
    public void handleEvent(Event event) {

    }
}
