package protocol;

import exceptions.NotReadyForInitException;
import interfaces.NeighbourhoodListener;
import interfaces.Node;
import network.Network;
import network.PersistantNetwork;

import java.net.InetSocketAddress;
import java.util.Set;

public interface PeerSamplingNode extends Node {

    Set<InetSocketAddress> activeView();

    Set<InetSocketAddress> passiveView();

    int setUnbiasedSize(int size) throws IllegalArgumentException;

    int setAViewSize(int size) throws IllegalArgumentException;

    int setPViewSize(int size) throws IllegalArgumentException;

    boolean setNeighbourhoodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException;

    boolean setNeighbourhoodListeners(Set<NeighbourhoodListener> listeners)
            throws IllegalArgumentException;

    boolean removeNeighbourboodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException;

    void initialize() throws NotReadyForInitException;

    void join(InetSocketAddress contact);
}
