package protocol.rework;

import interfaces.CostComparer;
import message.Message;
import message.xbot.ReplaceMessage;
import message.xbot.ReplaceReplyMessage;
import message.xbot.SwitchMessage;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

public class XBotDisco implements XBotSupportEdge {

    private InetSocketAddress cycle;
    private XBotMain xBotMain;
    private PersistantNetwork tcp;

    private long initToCand;
    private long initToOld;

    private InetSocketAddress old;
    private long disconnectToOld;

    private InetSocketAddress cand;
    private long candToDisconnect;

    XBotDisco(InetSocketAddress cycle, XBotMain xBotMain, PersistantNetwork tcp) {
        this.cycle = cycle;
        this.xBotMain = xBotMain;
        this.tcp = tcp;

        initToCand = 0;
        initToOld = 0;

        old = null;
        disconnectToOld = 0;

        cand = null;
        candToDisconnect = 0;
    }

    void handleReplace(ReplaceMessage replaceMessage) {
        cand = replaceMessage.sender();
        initToCand = replaceMessage.itoc();

        old = replaceMessage.old();
        initToOld = replaceMessage.itoo();

        try {
            xBotMain.getCost(replaceMessage.old(), this);
            xBotMain.getCost(replaceMessage.sender(), this);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleCost(InetSocketAddress peer, long cost) {
        if(peer.equals(cand)) {
            candToDisconnect = cost;

            if(old != null)
                reply();

            return true;
        } else if(peer.equals(old)) {
            disconnectToOld = cost;

            if(cand != null)
                reply();

            return true;
        } else
            return false;

    }

    private void reply() {
        // Just to make the code easier to read
        InetSocketAddress init = cycle;

        try {
            if(!xBotMain.canOptimize(cycle, old) || !xBotMain.canOptimize(cand, xBotMain.id())) {
                Message replaceReply = new ReplaceReplyMessage(xBotMain.id(), false);
                tcp.send(replaceReply.bytes(), cand);

                xBotMain.finishCycle(cycle);
                return;
            }

            if(itsWorthOptimizing(this::basicComparer, initToOld, initToCand, candToDisconnect, disconnectToOld)) {

                Message replaceReply = new ReplaceReplyMessage(xBotMain.id(), true);
                Message switchMessage = new SwitchMessage(xBotMain.id(), init, disconnectToOld);

                tcp.send(replaceReply.bytes(), cand);
                tcp.send(switchMessage.bytes(), old);

                xBotMain.removeFromBiased(cand);

                xBotMain.addPeerToBiasedActiveView(old, disconnectToOld);
            } else {
                Message replaceReply = new ReplaceReplyMessage(xBotMain.id(), false);
                tcp.send(replaceReply.bytes(), cand);
            }

            xBotMain.finishCycle(cycle);
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private boolean itsWorthOptimizing(CostComparer comparer, long itoo,
                                       long itoc, long ctod, long dtoo) {
        return comparer.compare(itoc, itoo, ctod, dtoo);
    }

    private boolean basicComparer(long itoo, long itoc, long ctod, long dtoo) {
        return itoc + dtoo < itoo + ctod;
    }
}
