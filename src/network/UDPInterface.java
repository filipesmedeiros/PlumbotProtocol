package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.MessageListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface UDPInterface {

    // Names for threads
    public static final String RECEIVE_THREAD = "Receive Thread";
    public static final String FULFILL_THREAD = "Fulfill Thread";

    // End of transmission
    public static final byte EOT = 4;

    void send(ByteBuffer bytes, InetSocketAddress to)
        throws IOException, InterruptedException, IllegalArgumentException;

    UDPInterface setMsgSize(int size);

    UDPInterface addMessageListener(MessageListener listener, List<Short> msgTypes);

    UDPInterface addMessageListeners(Map<MessageListener, List<Short>> listeners);

    void init()
        throws NotReadyForInitException;

    UDPInterface setRequestQueueSize(int size)
        throws CantResizeQueueException;
}
