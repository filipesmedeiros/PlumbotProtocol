package protocol.xbot;

import interfaces.CostComparer;
import message.Message;
import message.xbot.ReplaceMessage;
import message.xbot.ReplaceReplyMessage;
import message.xbot.SwitchMessage;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

class XBotDisco implements XBotSupportEdge {

    private InetSocketAddress cycle;
    private XBotNode xBotNode;
    private PersistantNetwork tcp;

    private long initToCand;
    private long initToOld;

    private InetSocketAddress old;
    private long disconnectToOld;

    private InetSocketAddress cand;
    private long candToDisconnect;

    XBotDisco(InetSocketAddress cycle, XBotNode xBotNode, PersistantNetwork tcp) {
        this.cycle = cycle;
        this.xBotNode = xBotNode;
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
            xBotNode.getCost(replaceMessage.old(), this);
            xBotNode.getCost(replaceMessage.sender(), this);
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
            if(xBotNode.cantOptimize(cycle, old) || xBotNode.cantOptimize(cand, xBotNode.id())) {
                Message replaceReply = new ReplaceReplyMessage(xBotNode.id(), init, false);
                tcp.send(replaceReply.bytes(), cand);

                xBotNode.finishCycle(cycle);
                return;
            }

            if(itsWorthOptimizing(this::basicComparer, initToOld, initToCand, candToDisconnect, disconnectToOld)) {

                Message replaceReply = new ReplaceReplyMessage(xBotNode.id(), init, true);
                Message switchMessage = new SwitchMessage(xBotNode.id(), init, disconnectToOld);

                tcp.send(replaceReply.bytes(), cand);
                tcp.send(switchMessage.bytes(), old);

                xBotNode.removeFromBiased(cand);

                xBotNode.addPeerToBiasedActiveView(old, disconnectToOld);
            } else {
                Message replaceReply = new ReplaceReplyMessage(xBotNode.id(), init, false);
                tcp.send(replaceReply.bytes(), cand);
            }

            xBotNode.finishCycle(cycle);
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
