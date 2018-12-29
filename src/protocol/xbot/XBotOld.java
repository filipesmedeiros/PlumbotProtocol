package protocol.xbot;

import common.TimerManager;

import java.net.InetSocketAddress;

class XBotOld implements XBotSupport {

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

    @Override
    public InetSocketAddress cycle() {
        return cycle;
    }

    void handleSwitch(InetSocketAddress disco, long discoToOld) {
        // To make code more readable
        InetSocketAddress init = cycle;

        xBotNode.movePeerToPassiveView(init);
        xBotNode.movePeerToActiveView(disco, discoToOld);

        timerManager.stop(XBotNode.WAIT + cycle.toString());
    }

    void handleDisconnectWait() {
        timerManager.addAction(XBotNode.WAIT + cycle.toString(),
                () -> xBotNode.fireConnectionTimeout(cycle), waitTimeout);
    }
}
