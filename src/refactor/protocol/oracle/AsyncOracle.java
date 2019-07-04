package refactor.protocol.oracle;

import java.net.InetSocketAddress;

import refactor.protocol.MessageListener;

public interface AsyncOracle extends MessageListener {

	void getCost(InetSocketAddress node);


}
