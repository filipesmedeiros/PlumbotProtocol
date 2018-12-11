package interfaces;

import java.net.InetSocketAddress;

public interface StableNeighbour {

    boolean canRemove(InetSocketAddress peer) throws IllegalArgumentException;
}
