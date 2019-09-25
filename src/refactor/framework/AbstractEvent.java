package refactor.framework;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEvent implements Event {

    private List<AbstractProtocol> passedByProtocols;

    private Priority priority;

    public AbstractEvent(Priority priority) {
        this.passedByProtocols = new ArrayList<>();
        this.priority = priority;
    }

    public AbstractEvent() {
        this(Priority.normal);
    }

    @Override
    public List<AbstractProtocol> passedBy(AbstractProtocol protocol) {
        passedByProtocols.add(protocol);
        return passedByProtocols;
    }

    @Override
    public List<AbstractProtocol> passedByProtocols() {
        return passedByProtocols;
    }

    @Override
    public Priority priority() {
        return priority;
    }

    @Override
    public int compareTo(Event event) {
        return priority.degree() - event.priority().degree();
    }
}
