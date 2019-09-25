package refactor.framework;

import refactor.message.Message;

public class MessageEvent extends DirectedEvent {

    private Message message;

    public MessageEvent(Priority priority, boolean directionIsUp, Message message) {
        super(priority, directionIsUp);
        this.message = message;
    }

    public MessageEvent(boolean directionIsUp, Message message) {
        this(Priority.normal, directionIsUp, message);
    }

    public Message message() {
        return message;
    }
}
