package common;

import java.util.Map;
import java.util.Timer;
import java.util.function.Consumer;

public interface TimerManager {

    void addTimer(String id, Runnable task, long interval);

    void addDelayedTimer(String id, Runnable task, long interval, long delay);

    void addAction(String id, Runnable task, long delay);

    void addAction(String id, Consumer<Object> task, Object t, long delay);

    Map<String, Timer> timers();

    Timer get(String id);

    void stop(String id);
}
