package common;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class MappedTimerManager implements TimerManager {

    private Map<String, Timer> timers;

    public MappedTimerManager() {
        timers = new HashMap<>();
    }

    @Override
    public void addTimer(String id, Runnable task, long interval) {
        createRunnableTimer(id, task, 0, interval);
    }

    @Override
    public void addDelayedTimer(String id, Runnable task, long delay, long interval) {
        createRunnableTimer(id, task, delay, interval);
    }

    @Override
    public void addAction(String id, Runnable task, long delay) {
        createRunnableTimer(id, task, delay, 0);
    }

    @Override
    public void addAction(String id, Consumer<Object> task, Object t, long delay) {
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                task.accept(t);
            }
        };

        schedule(id, tTask, delay, 0);
    }

    private void createRunnableTimer(String id, Runnable task, long delay, long interval) {
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        schedule(id, tTask, delay, interval);
    }

    private void schedule(String id, TimerTask tTask, long delay, long interval) {
        Timer timer = new Timer(id, true);
        timers.put(id, timer);

        if(interval == 0)
            timer.schedule(tTask, delay);
        else
            timer.schedule(tTask, delay, interval);
    }

    @Override
    public Map<String, Timer> timers() {
        return timers;
    }

    @Override
    public Timer get(String id) {
        return timers.get(id);
    }


    @Override
    public void stop(String id) {
        timers.get(id).cancel();
        timers.remove(id);
    }
}
