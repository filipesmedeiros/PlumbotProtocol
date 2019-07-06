package refactor.utils;

import refactor.message.MessageDecoder;
import refactor.protocol.notifications.Notifiable;

import java.net.InetSocketAddress;

public interface MessageListener extends Notifiable {

    void listenToMessage(MessageDecoder.MessageType messageType, InetSocketAddress sender);
}
