package common;

import java.util.function.Consumer;

public interface ConsumerTimerManager extends TimerManager {

    void addAction(String id, Consumer task, long delay);
}
