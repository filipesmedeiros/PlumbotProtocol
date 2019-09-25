package refactor.framework;

public class DirectedEvent extends AbstractEvent {

    // True means up, false means down
    private boolean directionIsUp;

    public DirectedEvent(Priority priority, boolean directionIsUp) {
        super(priority);
        this.directionIsUp = directionIsUp;
    }

    public DirectedEvent(boolean directionIsUp) {
        this(Priority.normal, directionIsUp);
    }

    public boolean isDirectionUp() {
        return directionIsUp;
    }
}
