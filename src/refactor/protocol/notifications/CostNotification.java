package refactor.protocol.notifications;

import java.net.InetSocketAddress;

public class CostNotification implements Notification {

    private long cost;

    private InetSocketAddress sender;

    public CostNotification(long cost, InetSocketAddress sender) {
        this.cost = cost;
        this.sender = sender;
    }

    public long cost() {
        return cost;
    }

    public InetSocketAddress sender() {
        return sender;
    }
}
