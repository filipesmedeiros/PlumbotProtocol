package refactor.protocol.oracle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import refactor.GlobalSettings;
import refactor.exception.SingletonIsNullException;
import refactor.message.Message;
import refactor.message.MessageDecoder;
import refactor.network.TCP;
import refactor.protocol.notifications.CostNotification;
import refactor.protocol.xbot.XBotNode;
import refactor.utils.BBInetSocketAddress;
import refactor.protocol.notifications.MessageNotification;
import refactor.protocol.notifications.Notification;
import refactor.utils.Utils;

public class RTTOracle extends AbstractAsyncOracle {
	
	private Map<InetSocketAddress, Long> sendTimes;

	private static RTTOracle rttOracle = new RTTOracle();

	public static RTTOracle getRttOracle() {
		return rttOracle;
	}
	
	private RTTOracle(int initialCapacity) {
		super(initialCapacity);
		sendTimes = new HashMap<>(initialCapacity);
	}

	private RTTOracle() {
		this(10);
	}

	@Override
	public void handleNotification(Notification notification) {
		if(notification instanceof CostRequestNotification)
			try {
				sendPing(((CostRequestNotification) notification).node);
			} catch(IOException ioe) {
				// TODO
				System.exit(1);
			}
		else if(notification instanceof MessageNotification)
			handleMessage(((MessageNotification) notification).message());
	}

	private void sendPing(InetSocketAddress node)
			throws IOException {
		Long sendTime = sendTimes.get(node);
		if(sendTime != null && Utils.timeElapsed(sendTime, TimeUnit.SECONDS) < 30) {
			if(GlobalSettings.DEBUGGING_LEVEL >= 4)
				System.out.println("Trying to ping the same node to often");
		}

		Message pingMessage = new Message(MessageDecoder.MessageType.ping)
				.withSender()
				.setDestination(node);
		try {
			TCP.tcp().notify(new MessageNotification(pingMessage));
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
		listenToMessage(MessageDecoder.MessageType.ping, node);
		sendTimes.put(node, System.nanoTime());
	}

	@Override
	public void handleMessage(Message message) {
		if(message.messageType() != MessageDecoder.MessageType.ping) {
			if(GlobalSettings.DEBUGGING_LEVEL >= 3)
				System.out.println("Wrong message received, expected ping back");
			return;
		}
		try {
			InetSocketAddress sender =
					BBInetSocketAddress.fromByteBuffer(message.metadataEntry(Message.SENDER_LABEL));
			Long sendTime = sendTimes.get(sender);
			if(sendTime == null) {
				if(GlobalSettings.DEBUGGING_LEVEL >= 4)
					System.out.println("This oracle wasn't waiting for a ping back from this node");
				return;
			}
			XBotNode.xBotNode().notify(new CostNotification(Utils.timeElapsedNano(sendTime), sender));
			sendTimes.remove(sender);
		} catch(UnknownHostException uhe) {
			// TODO
			System.exit(1);
		}
	}
}
