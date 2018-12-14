package interfaces;

import notifications.Notification;

public interface Notifiable {

    void notify(Notification notification) throws InterruptedException;
}
