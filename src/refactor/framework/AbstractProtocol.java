package refactor.framework;

import refactor.GlobalSettings;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public abstract class AbstractProtocol implements Protocol {

    private BlockingQueue<Event> events;

    private Protocol upProtocol;

    private Protocol downProtocol;

    public AbstractProtocol() {
        events = new PriorityBlockingQueue<>();
        GlobalSettings.FIXED_THREAD_POOL.submit(this::dequeueEvents);
    }

    @Override
    public void up(EventBatch eventBatch) {
        eventBatch.forEach(event -> {
            if(!checkEvent(event))
                eventBatch.remove(event);
        });
        upProtocol.down(eventBatch);
    }

    @Override
    public void up(Event... events) {
        EventBatch eventBatch = new EventBatch();
        for(Event event : events)
            if(checkEvent(event))
                eventBatch.add(event);
         upProtocol.down(eventBatch);
    }

    @Override
    public void down(EventBatch eventBatch) {
        eventBatch.forEach(event -> {
            if(!checkEvent(event))
                eventBatch.remove(event);
        });
        downProtocol.up(eventBatch);
    }

    @Override
    public void down(Event... events) {
        EventBatch eventBatch = new EventBatch();
        for(Event event : events)
            if(checkEvent(event))
                eventBatch.add(event);
        downProtocol.up(eventBatch);
    }

    @Override
    public Protocol upProtocol() {
        return upProtocol;
    }

    @Override
    public Protocol upProtocol(Protocol protocol) {
        Protocol oldProtocol = upProtocol;
        upProtocol = protocol;
        return oldProtocol;
    }

    @Override
    public Protocol downProtocol() {
        return downProtocol;
    }

    @Override
    public Protocol downProtocol(Protocol protocol) {
        Protocol oldProtocol = downProtocol;
        downProtocol = protocol;
        return oldProtocol;
    }

    @Override
    public void queueEvent(Event event) {
        try {
            events.put(event);
        } catch(InterruptedException ie) {
            // TODO
            ie.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void dequeueEvents() {
        try {
            for (;;) {
                Event event = events.take();
                handleEvent(event);
            }
        } catch(InterruptedException ie) {
            // TODO
            ie.printStackTrace();
            System.exit(1);
        }
    }
}
