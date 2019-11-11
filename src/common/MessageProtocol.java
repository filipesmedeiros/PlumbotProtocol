package common;

import messages.Message;

public interface MessageProtocol {

    void deliverMessage(Message m);
}
