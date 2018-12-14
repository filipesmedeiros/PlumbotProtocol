package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.Node;
import interfaces.OnlineNotifiable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface NetworkInterface {

    // Names for threads
    String LISTEN_TO_SELECTOR_THREAD = "Listen To Selector Thread";
    // String FULFILL_THREAD = "Fulfill Thread";

    // End of transmission
    byte EOT = 4;

    void send(ByteBuffer bytes, InetSocketAddress to)
        throws IOException, InterruptedException, IllegalArgumentException;

    NetworkInterface setMsgSize(int size);

    NetworkInterface addMessageListener(OnlineNotifiable listener, List<Short> msgTypes);

    NetworkInterface addMessageListeners(Map<OnlineNotifiable, List<Short>> listeners);

    void init()
        throws NotReadyForInitException;

    NetworkInterface setRequestQueueSize(int size)
        throws CantResizeQueueException;
}
