package refactor.protocol.oracle;

import java.net.InetSocketAddress;

import refactor.utils.NotificationListener;

public interface AsyncOracle extends NotificationListener {

	void getCost(InetSocketAddress node);
}
