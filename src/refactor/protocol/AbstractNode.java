package refactor.protocol;

import refactor.utils.RoutedMessageListener;

public abstract class AbstractNode extends RoutedMessageListener implements Node {

    public AbstractNode() {
        this(10);
    }

    public AbstractNode(int initialCapacity) {
        super(initialCapacity);
    }
}
