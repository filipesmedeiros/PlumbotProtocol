package refactor.utils;

import java.util.concurrent.TimeUnit;

/**
 * Simple static class to store useful methods
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 20.06.2019
 */
public class Utils {

    public static long timeElapsedNano(long since) {
        return System.nanoTime() - since;
    }

    public static long timeElapsed(long since, TimeUnit timeUnit) {
        return timeUnit.convert(timeElapsedNano(since), TimeUnit.NANOSECONDS);
    }
}
