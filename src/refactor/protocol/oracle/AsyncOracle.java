package refactor.protocol.oracle;

import java.net.InetSocketAddress;

import refactor.utils.MessageListener;

public interface AsyncOracle extends MessageListener {

	void getCost(InetSocketAddress node);
}
