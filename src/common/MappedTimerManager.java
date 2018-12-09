package common;

import javax.print.DocFlavor;
import javax.print.attribute.standard.RequestingUserName;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MappedTimerManager implements TimerManager {

    private Map<String, Timer> timers;

    public MappedTimerManager() {
        timers = new HashMap<>();
    }

    @Override
    public void addTimer(String id, Runnable task, long interval) {
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        Timer timer = new Timer(id, true);
        timers.put(id, timer);
        timer.schedule(tTask, 0, interval);
    }

    @Override
    public void addDelayedTimer(String id, Runnable task, long interval, long delay) {
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        Timer timer = new Timer(id, true);
        timers.put(id, timer);
        timer.schedule(tTask, delay, interval);
    }

    @Override
    public void addAction(String id, Runnable task, long delay) {
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        Timer timer = new Timer(id, true);
        timers.put(id, timer);
        timer.schedule(tTask, delay);
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
