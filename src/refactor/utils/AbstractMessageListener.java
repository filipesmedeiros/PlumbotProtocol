package refactor.utils;

import refactor.exception.SingletonIsNullException;
import nettyFoutoRefactor.network.messaging.MessageSerializer;
import nettyFoutoRefactor.network.messaging.MessageRouter;
import refactor.protocol.notifications.AbstractNotifiable;

import java.net.InetSocketAddress;

public abstract class AbstractMessageListener extends AbstractNotifiable implements MessageListener {

    public AbstractMessageListener() {
        this(10);
    }

    public AbstractMessageListener(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void listenToMessage(MessageSerializer.MessageType messageType, InetSocketAddress sender) {
        try {
            MessageRouter.getRouter().addRoute(this, messageType, sender);
        } catch(SingletonIsNullException sine) {
            // TODO
            System.exit(1);
        }
    }
}
