package refactor.message;

import refactor.GlobalSettings;
import refactor.exception.SingletonIsNullException;
import refactor.utils.NotificationListener;
import refactor.utils.BBInetSocketAddress;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// TODO Javadoc this class
public class MessageRouter {

	private static final int DEFAULT_MESSAGE_QUEUE_SIZE = 20;

	private BlockingQueue<Message> messagesToDeliver;

	private Map<MessageRoutingKey, NotificationListener> routes;

	private static final MessageRouter router = new MessageRouter();

	public static MessageRouter getRouter()
			throws SingletonIsNullException {
		if(router == null)
			throw new SingletonIsNullException(MessageRouter.class.getName());
		return router;
	}

	private MessageRouter() {
		messagesToDeliver = new ArrayBlockingQueue<>(DEFAULT_MESSAGE_QUEUE_SIZE);
		routes = new HashMap<>();

		GlobalSettings.FIXED_THREAD_POOL.submit(this::routeMessages);
	}

	private void routeMessages() {
		try {
			Message message = messagesToDeliver.take();
			MessageRoutingKey mrk = MessageRoutingKey.fromMessage(message);
			new Thread(() -> {
				try {
					routes.get(mrk).notify(message);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			routes.remove(mrk);
		} catch(InterruptedException | UnknownHostException | IllegalArgumentException ite) {
			// TODO
			System.exit(1);
		}
	}

	public boolean deliverMessage(Message messageWithSender) {
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

	public void addRoute(NotificationListener notificationListener,
                         MessageDecoder.MessageType messageType, InetSocketAddress sender) {
		routes.put(new MessageRoutingKey(messageType, sender), notificationListener);
	}

	private static class MessageRoutingKey {

		private MessageDecoder.MessageType messageType;
		private InetSocketAddress sender;

		private MessageRoutingKey(MessageDecoder.MessageType messageType, InetSocketAddress sender) {
			this.messageType = messageType;
			this.sender = sender;
		}

		private static MessageRoutingKey fromMessage(Message message)
				throws UnknownHostException, IllegalArgumentException {
			InetSocketAddress sender = BBInetSocketAddress.fromByteBuffer(
					message.metadataEntry(Message.SENDER_LABEL));
			return new MessageRoutingKey(message.messageType(), sender);
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
