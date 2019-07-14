package refactor.framework;

import java.nio.ByteBuffer;

public interface TransportProtocol extends Protocol {

    void sendBytes(ByteBuffer byteBuffer);

    void receiveBytes(ByteBuffer byteBuffer);
}
