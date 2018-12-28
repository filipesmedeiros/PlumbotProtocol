package protocol.xbot;

import common.TimerManager;

import java.net.InetSocketAddress;

class XBotOld implements XBotSupport {

    private static final String WAIT = "W";

    private InetSocketAddress cycle;
    private XBotNode xBotNode;

    private TimerManager timerManager;
    private long waitTimeout;

    XBotOld(InetSocketAddress cycle, XBotNode xBotNode, TimerManager timerManager, long waitTimeout) {
        this.cycle = cycle;
        this.xBotNode = xBotNode;

        this.timerManager = timerManager;
        this.waitTimeout = waitTimeout;
    }

    void handleSwitch(InetSocketAddress disco, long discoToOld) {
        // To make code more readable
        InetSocketAddress init = cycle;

        xBotNode.removeFromActive(init);
        xBotNode.addPeerToBiasedActiveView(disco, discoToOld);
    }

    void handleDisconnectWait() {
        timerManager.addAction(WAIT, () -> xBotNode.setWaitingFalse(), waitTimeout);
    }
}
