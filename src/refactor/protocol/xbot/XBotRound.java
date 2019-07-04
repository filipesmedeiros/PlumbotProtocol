package refactor.protocol.xbot;

import java.net.InetSocketAddress;

class XBotRound {
	
	enum Role {
		initiator,
		old,
		candidate,
		disconnected
	}

	InetSocketAddress initiator;
	InetSocketAddress old;
	InetSocketAddress candidate;
	InetSocketAddress disconnected;
}
