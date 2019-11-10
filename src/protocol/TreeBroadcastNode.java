package protocol;

import exceptions.NotReadyForInitException;
import interfaces.NeighbourhoodListener;
import interfaces.NetworkNotifiable;
import interfaces.StableNeighbour;
import test.Application;

import java.nio.ByteBuffer;
import java.util.Set;

public interface TreeBroadcastNode extends NeighbourhoodListener, NetworkNotifiable, StableNeighbour {

    void broadcast(ByteBuffer bytes) throws IllegalArgumentException;

    int setNumberOfEagerPeers(int size) throws IllegalArgumentException;

    int setNumberOfLazyPeers(int size) throws IllegalArgumentException;

    void addApplication(Application app) throws IllegalArgumentException;

    void addApplications(Set<Application> apps) throws IllegalArgumentException;

    void initialize() throws NotReadyForInitException;
}
