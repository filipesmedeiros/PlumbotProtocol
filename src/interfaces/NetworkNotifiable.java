package interfaces;

import network.NetworkInterface;

public interface NetworkNotifiable extends Notifiable {

    boolean setNetwork(NetworkInterface network);
}
