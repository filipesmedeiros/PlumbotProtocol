package events;

public interface EventHandler {

    void receive(Event e);

    void handle(Event e);
}
