package refactor.utils;

import refactor.message.Message;

public class MessageNotification implements Notification {

    private Message message;

    public MessageNotification(Message message) {
        this.message = message;
    }

    public Message message() {
        return message;
    }
}
