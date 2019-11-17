package xbot.timers;

import babel.timer.ProtocolTimer;

public class OptimizationTimer extends ProtocolTimer {

    public static final short TIMER_CODE = 201;

    public OptimizationTimer() {
        super(TIMER_CODE);
    }

    @Override
    public Object clone() {
        return this;
    }
}
