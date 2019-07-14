package refactor.framework;

public interface Protocol {

    void up(EventBatch eventBatch);

    void up(Event... events);

    void down(EventBatch eventBatch);

    void down(Event... events);

    Protocol upProtocol();

    Protocol upProtocol(Protocol protocol);

    Protocol downProtocol();

    Protocol downProtocol(Protocol protocol);

    boolean checkEvent(Event event);

    void queueEvent(Event event);

    void dequeueEvents();

    void handleEvent(Event event);
}
