package refactor.protocol.notifications;

import nettyFoutoRefactor.network.messaging.Message;

public class MessageNotification implements Notification {

    private Message message;

    public MessageNotification(Message message) {
        this.message = message;
    }

    public Message message() {
        return message;
    }
}
