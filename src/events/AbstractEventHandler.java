package events;

import common.concurrent.ThreadPool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractEventHandler implements EventHandler {

    private BlockingQueue<Event> events;

    public AbstractEventHandler() {
        events = new ArrayBlockingQueue<>(10);

        ThreadPool.getInstance().submit(() -> {
            while(true)
                try {
                    handle(events.take());
                } catch (InterruptedException ie) {
                    ie.printStackTrace();

                    // TODO
                }
        });
    }


    @Override
    public void receive(Event e) {
        try {
            events.put(e);
        } catch(InterruptedException ie) {
            ie.printStackTrace();

            // TODO
        }
    }
}
