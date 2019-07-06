package refactor.protocol.oracle;

import refactor.utils.AbstractMessageListener;
import refactor.protocol.notifications.Notification;

import java.net.InetSocketAddress;

public abstract class AbstractAsyncOracle extends AbstractMessageListener implements AsyncOracle {

    public AbstractAsyncOracle(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void getCost(InetSocketAddress node) {
        notify(new CostRequestNotification(node));
    }

    protected static class CostRequestNotification implements Notification {

        protected InetSocketAddress node;

        private CostRequestNotification(InetSocketAddress node) {
            this.node = node;
        }
    }
}
