package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.NetworkNotifiable;

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

    NetworkInterface addMessageListener(NetworkNotifiable listener, List<Short> msgTypes);

    NetworkInterface addMessageListeners(Map<NetworkNotifiable, List<Short>> listeners);

    void initialize()
        throws NotReadyForInitException, IOException;

    NetworkInterface setRequestQueueSize(int size)
        throws CantResizeQueueException;
}
