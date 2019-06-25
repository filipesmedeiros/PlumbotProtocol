package refactor.protocol;

import refactor.message.Message;

public interface MessageListener {

    void notify(Message message);
}
