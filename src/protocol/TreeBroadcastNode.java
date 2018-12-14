package protocol;

import exceptions.NotReadyForInitException;
import interfaces.NeighbourhoodListener;
import interfaces.OnlineNotifiable;
import interfaces.StableNeighbour;
import test.Application;

import java.nio.ByteBuffer;
import java.util.Set;

public interface TreeBroadcastNode extends NeighbourhoodListener, OnlineNotifiable, StableNeighbour {

    void broadcast(ByteBuffer bytes) throws IllegalArgumentException;

    int eagerPeerSetSize(int size) throws IllegalArgumentException;

    int lazyPeerSetSize(int size) throws IllegalArgumentException;

    void addApplication(Application app) throws IllegalArgumentException;

    void addApplications(Set<Application> apps) throws IllegalArgumentException;

    void initialize() throws NotReadyForInitException;
}
