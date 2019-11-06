package events;

public interface EventEmitter {

    void emit(Event e, EventHandler handler);
}
