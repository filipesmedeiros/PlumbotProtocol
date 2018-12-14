package interfaces;

import network.Network;

import java.net.InetSocketAddress;

public interface Node extends Notifiable {

    InetSocketAddress id();

    boolean setNetwork(Network network);
}
