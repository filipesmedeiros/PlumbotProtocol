package interfaces;

import notifications.Notification;

import java.net.InetSocketAddress;

public interface Node {

    InetSocketAddress id();

    void notify(Notification notification);
}
