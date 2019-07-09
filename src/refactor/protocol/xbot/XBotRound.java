package refactor.protocol.xbot;

import java.net.InetSocketAddress;

class XBotRound {
	
	enum Role {
		initiator,
		candidate,
		old,
		disconnected
	}

	InetSocketAddress initiator;
	InetSocketAddress old;
	InetSocketAddress candidate;
	InetSocketAddress disconnected;

	public XBotRound addRole(Role role, InetSocketAddress node) {
		switch(role) {
			case candidate:
				candidate = node;
				break;
			case old:
				old = node;
				break;
			case disconnected:
				disconnected = node;
				break;
		}
		return this;
	}
}
