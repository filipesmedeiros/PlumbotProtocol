package nettyFoutoRefactor.network.messaging;

import refactor.GlobalSettings;
import refactor.exception.SingletonIsNullException;
import refactor.protocol.notifications.MessageNotification;
import refactor.protocol.notifications.Notifiable;
import refactor.utils.BBInetSocketAddress;
import refactor.utils.MessageListener;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

// TODO Javadoc this class
public class MessageRouter {

	private static final int DEFAULT_MESSAGE_QUEUE_SIZE = 20;

	private Map<MessageRoutingKey, MessageListener> routes;

	private static final MessageRouter router = new MessageRouter();

	public static MessageRouter getRouter()
			throws SingletonIsNullException {
		if(router == null)
			throw new SingletonIsNullException(MessageRouter.class.getName());
		return router;
	}

	private MessageRouter() {
		routes = new HashMap<>();
	}

	public void routeMessage(Message message) {
		try {
			MessageRoutingKey mrk = MessageRoutingKey.createKey(message);
			Notifiable notifiable = routes.get(mrk);
			if(notifiable == null) {
				if(GlobalSettings.DEBUGGING_LEVEL >= 4)
					System.out.println("No MessageListener was waiting for that message");
				return;
			}
			routes.get(mrk).notify(new MessageNotification(message));
			routes.remove(mrk);
		} catch(UnknownHostException | IllegalArgumentException ite) {
			// TODO
			System.exit(1);
		}
	}

	public void addRoute(MessageListener messageListener,
						 MessageSerializer.MessageType messageType, InetSocketAddress sender) {
		routes.put(new MessageRoutingKey(messageType, sender), messageListener);
	}

	private static class MessageRoutingKey {

		private MessageSerializer.MessageType messageType;
		private InetSocketAddress sender;

		private MessageRoutingKey(MessageSerializer.MessageType messageType, InetSocketAddress sender) {
			this.messageType = messageType;
			this.sender = sender;
		}

		private static MessageRoutingKey createKey(Message message)
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
