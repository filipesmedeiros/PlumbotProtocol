package protocol;

import exceptions.NotReadyForInitException;
import interfaces.MessageListener;
import interfaces.NeighbourhoodListener;

import java.nio.ByteBuffer;

public interface TreeBroadcastNode extends NeighbourhoodListener, MessageListener {

    void broadcast(ByteBuffer buffer) throws IllegalArgumentException;

    int eagerPeerSetSize(int size) throws IllegalArgumentException;

    int lazyPeerSetSize(int size) throws IllegalArgumentException;

    void initialize() throws NotReadyForInitException;
}
