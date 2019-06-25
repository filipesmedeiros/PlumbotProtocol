package refactor.message;

import refactor.GlobalSettings;
import refactor.protocol.MessageListener;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// TODO Javadoc this class
public class MessageRouter {

    private static final int DEFAULT_MESSAGE_QUEUE_SIZE = 30;

    private BlockingQueue<MessageWithSender> messagesToDeliver;

    private Map<MessageRoutingKey, MessageListener> routes;

    private static MessageRouter router = new MessageRouter();

    public static MessageRouter getRouter() {
        return router;
    }

    private MessageRouter() {
        messagesToDeliver = new ArrayBlockingQueue<>(DEFAULT_MESSAGE_QUEUE_SIZE);
        routes = new HashMap<>();

        GlobalSettings.FIXED_THREAD_POOL.submit(this::routeMessages);
    }

    private void routeMessages() {
        try {
            MessageWithSender messageWithSender = messagesToDeliver.take();

            getRoute(messageWithSender).notify(messageWithSender);
            MessageRoutingKey messageRoutingKey =
                    new MessageRoutingKey(messageWithSender.messageType(), messageWithSender.sender());
            routes.remove(messageRoutingKey);
        } catch(InterruptedException ite) {
            // TODO
            System.exit(1);
        }
    }

    public boolean deliverMessage(MessageWithSender messageWithSender) {
        if(messageWithSender == null)
            return false;
        try {
            messagesToDeliver.put(messageWithSender);
            return true;
        } catch(InterruptedException ite) {
            // TODO
            System.exit(1);
            return false;
        }
    }

    public void addRoute(MessageListener messageListener,
                                MessageDecoder.MessageType messageType, InetSocketAddress sender) {
        routes.put(new MessageRoutingKey(messageType, sender), messageListener);
    }

    public MessageListener getRoute(MessageWithSender messageToDeliver) {
        MessageRoutingKey messageRoutingKey =
                new MessageRoutingKey(messageToDeliver.messageType(), messageToDeliver.sender());
        return routes.get(messageRoutingKey);
    }

    private static class MessageRoutingKey {

        private MessageDecoder.MessageType messageType;
        private InetSocketAddress sender;

        private MessageRoutingKey(MessageDecoder.MessageType messageType, InetSocketAddress sender) {
            this.messageType = messageType;
            this.sender = sender;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof MessageRoutingKey))
                return false;
            MessageRoutingKey otherMessageRoutingKey = (MessageRoutingKey) other;
            if(otherMessageRoutingKey.messageType != messageType)
                return false;
            return otherMessageRoutingKey.sender.equals(sender);
        }
    }
}
