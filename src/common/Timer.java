package common;

public class Timer {

    private static java.util.Timer ourInstance = new java.util.Timer();

    public static java.util.Timer getInstance() {
        return ourInstance;
    }
}
