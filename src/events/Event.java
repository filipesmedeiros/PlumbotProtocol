package events;

public interface Event {

    enum EventType {
        MessageReceived;
    }

    String name();

    EventEmitter emitter();
}
