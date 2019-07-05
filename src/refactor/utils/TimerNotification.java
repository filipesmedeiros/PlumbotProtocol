package refactor.utils;

public class TimerNotification implements Notification {

    private Runnable task;

    public TimerNotification(Runnable task) {
        this.task = task;
    }

    public Runnable task() {
        return task;
    }

    public void runTask() {
        task.run();
    }
}
