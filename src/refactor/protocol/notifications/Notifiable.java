package refactor.protocol.notifications;

public interface Notifiable {

    void notify(Notification notification);

    void takeNotification();

    void handleNotification(Notification notification);
}
