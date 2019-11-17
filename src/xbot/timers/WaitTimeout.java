package xbot.timers;

import babel.timer.ProtocolTimer;
import network.Host;

public class WaitTimeout extends ProtocolTimer {

    public static final short TIMER_CODE = 202;

    private Host peer;

    public WaitTimeout(Host peer) {
        super(TIMER_CODE);
        this.peer = peer;
    }

    public Host peer() {
        return peer;
    }

    @Override
    public Object clone() {
        return new WaitTimeout(this.peer);
    }
}
