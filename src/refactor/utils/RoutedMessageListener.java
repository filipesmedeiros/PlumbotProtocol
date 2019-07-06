package refactor.utils;

import refactor.exception.SingletonIsNullException;
import refactor.message.MessageDecoder;
import refactor.message.MessageRouter;
import refactor.protocol.notifications.AbstractNotifiable;

import java.net.InetSocketAddress;

public class RoutedMessageListener extends AbstractNotifiable implements MessageListener {

    public RoutedMessageListener() {
        this(10);
    }

    public RoutedMessageListener(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void listenToMessage(MessageDecoder.MessageType messageType, InetSocketAddress sender) {
        try {
            MessageRouter.getRouter().addRoute(this, messageType, sender);
        } catch (SingletonIsNullException e) {
            // TODO
            System.exit(1);
        }
    }
}
