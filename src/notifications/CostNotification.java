package notifications;

import java.net.InetSocketAddress;

public class CostNotification implements Notification {

    public final static short TYPE = 3;

    private InetSocketAddress sender;

    private long cost;

    public CostNotification(InetSocketAddress sender, long cost) {
        this.sender = sender;
        this.cost = cost;
    }

    @Override
    public short type() {
        return 0;
    }

    public InetSocketAddress sender() {
        return sender;
    }

    public long cost() {
        return cost;
    }
}
