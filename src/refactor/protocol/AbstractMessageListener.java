package refactor.protocol;

import java.net.InetSocketAddress;

import refactor.exception.SingletonIsNullException;
import refactor.message.MessageRouter;
import refactor.message.MessageDecoder.MessageType;

public abstract class AbstractMessageListener implements MessageListener {
	
	@Override
	public void listenToMessage(MessageType messageType, InetSocketAddress sender) {
		try {
			MessageRouter.getRouter().addRoute(this, messageType, sender);
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
	}
}
