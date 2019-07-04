package refactor.protocol;

import java.net.InetSocketAddress;

import refactor.message.Message;
import refactor.message.MessageDecoder.MessageType;

public interface MessageListener {

    void notify(Message message);
    
    void listenToMessage(MessageType messageType, InetSocketAddress sender);
}
