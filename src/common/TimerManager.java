package common;

import java.util.Map;
import java.util.Timer;

public interface TimerManager {

    void addTimer(String id, Runnable task, long interval);

    void addDelayedTimer(String id, Runnable task, long interval, long delay);

    void addAction(String id, Runnable task, long delay);

    Map<String, Timer> timers();

    Timer get(String id);

    void stop(String id);
}
