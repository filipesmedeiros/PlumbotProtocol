package refactor.utils;

import nettyFoutoRefactor.network.messaging.Message;
import nettyFoutoRefactor.network.messaging.MessageSerializer;
import refactor.protocol.notifications.Notifiable;

import java.net.InetSocketAddress;

public interface MessageListener extends Notifiable {

    void listenToMessage(MessageSerializer.MessageType messageType, InetSocketAddress sender);

    void handleMessage(Message message);
}
