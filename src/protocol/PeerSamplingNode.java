package protocol;

import exceptions.NotReadyForInitException;
import interfaces.NeighbourhoodListener;
import interfaces.Node;
import interfaces.NetworkNotifiable;

import java.net.InetSocketAddress;
import java.util.Set;

public interface PeerSamplingNode extends Node, NetworkNotifiable {

    Set<InetSocketAddress> activeView();

    Set<InetSocketAddress> passiveView();

    int setUnbiasedSize(int size) throws IllegalArgumentException;

    int setActiveViewSize(int size) throws IllegalArgumentException;

    int setPassiveViewSize(int size) throws IllegalArgumentException;

    boolean setNeighbourhoodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException;

    boolean setNeighbourhoodListeners(Set<NeighbourhoodListener> listeners)
            throws IllegalArgumentException;

    boolean removeNeighbourboodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException;

    void initialize() throws NotReadyForInitException;

    void join(InetSocketAddress contact);

    void leave();
}
