package interfaces;

import network.NetworkInterface;

import java.nio.ByteBuffer;

public interface MessageListener {

    void notifyMessage(ByteBuffer msg);

    boolean setUDP(NetworkInterface udp) throws IllegalArgumentException;
}
