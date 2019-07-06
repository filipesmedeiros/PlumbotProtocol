package refactor.protocol.notifications;

public class CostNotification implements Notification {

    private long cost;

    public CostNotification(long cost) {
        this.cost = cost;
    }

    public long cost() {
        return cost;
    }
}
