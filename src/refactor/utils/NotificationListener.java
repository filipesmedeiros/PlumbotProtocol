package refactor.utils;

import java.net.InetSocketAddress;

import refactor.message.Message;
import refactor.message.MessageDecoder.MessageType;
import refactor.utils.Notification;

public interface NotificationListener {

    void notify(Notification message);
    
    void listenToMessage(MessageType messageType, InetSocketAddress sender);
}
