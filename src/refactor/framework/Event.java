package refactor.framework;

import java.util.List;

public interface Event extends Comparable<Event> {

    // For readability, I used an enum, but the best way to use priorities is
    // using integers, so ordinal() is used. This means that the priorities must
    // always be ordered from lowest to highest (top down)
    enum Priority {
        normal,
        top;

        public int degree() {
            return this.ordinal();
        }
    }

    List<Protocol> passedBy(Protocol protocol);

    List<Protocol> passedByProtocols();

    Priority priority();
}
