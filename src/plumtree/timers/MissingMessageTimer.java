package plumtree.timers;

import babel.timer.ProtocolTimer;

public class MissingMessageTimer extends ProtocolTimer {

    public static final short TIMER_CODE = 101;


    public MissingMessageTimer() {
        super(TIMER_CODE);
    }

    @Override
    public Object clone() {
        return this;
    }
}
