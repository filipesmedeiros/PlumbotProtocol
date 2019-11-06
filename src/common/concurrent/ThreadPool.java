package common.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    private static ExecutorService ourInstance = Executors.newCachedThreadPool();

    public static ExecutorService getInstance() {
        return ourInstance;
    }
}
