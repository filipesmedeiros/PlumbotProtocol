package refactor.framework;

import java.util.HashSet;

public class EventBatch extends HashSet<Event> {

    public EventBatch() {
        super();
    }

    public EventBatch(Event event) {
        super();
        add(event);
    }
}
