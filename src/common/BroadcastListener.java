package common;

import java.nio.ByteBuffer;

public interface BroadcastListener {

    void deliver(ByteBuffer m);
}
