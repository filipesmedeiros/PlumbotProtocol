package protocol.xbot;

import message.Message;
import message.xbot.OptimizationMessage;
import message.xbot.OptimizationReplyMessage;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

class XBotInit implements XBotSupportEdge {

    private InetSocketAddress cycle;
    private XBotNode xBotNode;
    private PersistantNetwork tcp;

    private InetSocketAddress cand;
    private InetSocketAddress old;
    private long initToOld;

    XBotInit(XBotNode xBotNode, PersistantNetwork tcp) {
        this.cycle = xBotNode.id();
        this.xBotNode = xBotNode;
        this.tcp = tcp;

        cand = null;
        old = null;
        initToOld = 0;
    }

    @Override
    public InetSocketAddress cycle() {
        return cycle;
    }

    boolean optimize() {
        if(xBotNode.isBiasedActiveViewEmpty())
            return false;

        XBotNode.BiasedInetAddress maybeThisOne = xBotNode.worstBiasedPeer();
        while(maybeThisOne != null && xBotNode.cantOptimize(xBotNode.id(), maybeThisOne.address))
            maybeThisOne = xBotNode.beforeWorstBiasedPeer(maybeThisOne);

        if(maybeThisOne == null)
            return false;

        old = maybeThisOne.address;

        cand = xBotNode.chooseCand();

        try {
            xBotNode.getCost(cand, this);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }

        return true;
    }

    void handleOptimizationReply(OptimizationReplyMessage optimizationReplyMessage) {
        if(optimizationReplyMessage.accept()) {
            xBotNode.movePeerToPassiveView(old);
            xBotNode.movePeerToActiveView(cand);
        }

        xBotNode.finishCycle(cycle);
    }

    @Override
    public boolean handleCost(InetSocketAddress peer, long cost) {
        if(peer.equals(cand)) {
            if(xBotNode.isBiasedActiveViewEmpty())
                return false;

            Message msg = new OptimizationMessage(xBotNode.id(), this.old, initToOld, cost);

            try {
                tcp.send(msg.bytes(), cand);
            } catch(IllegalArgumentException | IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }

        return false;
    }
}
