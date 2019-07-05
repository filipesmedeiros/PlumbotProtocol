package refactor.protocol;

import refactor.utils.NotificationListener;

public interface Node extends NotificationListener {

    void handleNotifications();
}
