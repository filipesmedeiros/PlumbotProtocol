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

    XBotInit(XBotNode xBotNode, PersistantNetwork tcp) {
        this.cycle = xBotNode.id();
        this.xBotNode = xBotNode;
        this.tcp = tcp;

        cand = null;
    }

    void optimize() {
        if(xBotNode.isBiasedActiveViewEmpty())
            return;

        cand = xBotNode.chooseCand();

        try {
            xBotNode.getCost(cand, this);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    void handleOptimizationReply(OptimizationReplyMessage optimizationReplyMessage) {

    }

    @Override
    public boolean handleCost(InetSocketAddress peer, long cost) {
        if(peer.equals(cand)) {
            if(xBotNode.isBiasedActiveViewEmpty())
                return false;

            XBotNode.BiasedInetAddress old = xBotNode.worstBiasedPeer();

            if(xBotNode.cantOptimize(xBotNode.id(), old.address)) {
                xBotNode.finishCycle(cycle);
                return false;
            }

            Message msg = new OptimizationMessage(xBotNode.id(), old.address, old.cost, cost);

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
