package refactor.protocol.oracle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import refactor.exception.SingletonIsNullException;
import refactor.message.Message;
import refactor.message.MessageDecoder.MessageType;
import refactor.network.TCP;
import refactor.utils.AbstractNotificationListener;

public class RTTOracle extends AbstractNotificationListener implements AsyncOracle {
	
	private Map<InetSocketAddress, Long> sendTimes;
	
	public RTTOracle() {
		sendTimes = new HashMap<>();
	}

	@Override
	public void notify(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getCost(InetSocketAddress node) {
		if(sendTimes.get(node) != null)
			return;
		Message pingMessage = new Message(MessageType.ping);
		pingMessage.setDestination(node);
		try {
			TCP.getTCP().sendMessage(pingMessage);
			sendTimes.put(node, System.currentTimeMillis());
		} catch (IOException | SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
	}
}
