package interfaces;

import java.nio.ByteBuffer;

public interface MessageListener {

    void notifyMessage(ByteBuffer msg);
}
