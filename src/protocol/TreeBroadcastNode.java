package protocol;

import interfaces.NeighbourhoodListener;

import java.nio.ByteBuffer;

public interface TreeBroadcastNode extends NeighbourhoodListener {

    void broadcast(ByteBuffer buffer) throws IllegalArgumentException;

    int setEagerSize(int size) throws IllegalArgumentException;

    int setLazySize(int size) throws IllegalArgumentException;
}
