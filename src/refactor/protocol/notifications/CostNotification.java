package refactor.protocol.notifications;

import java.net.InetSocketAddress;

public class CostNotification implements Notification {

    private long cost;

    private InetSocketAddress node;

    public CostNotification(long cost, InetSocketAddress sender) {
        this.cost = cost;
        this.node = sender;
    }

    public long cost() {
        return cost;
    }

    public InetSocketAddress node() {
        return node;
    }
}
