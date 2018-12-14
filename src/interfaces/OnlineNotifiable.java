package interfaces;

import network.NetworkInterface;

public interface OnlineNotifiable extends Notifiable {

    boolean setNetwork(NetworkInterface network);
}
