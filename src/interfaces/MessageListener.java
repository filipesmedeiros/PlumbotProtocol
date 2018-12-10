package interfaces;

import network.UDPInterface;

import java.nio.ByteBuffer;

public interface MessageListener {

    void notifyMessage(ByteBuffer msg);

    boolean setUDP(UDPInterface udp) throws IllegalArgumentException;
}
