package protocol.rework;

import message.Message;
import message.xbot.OptimizationMessage;
import message.xbot.OptimizationReplyMessage;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

public class XBotInit implements XBotSupportEdge {

    private InetSocketAddress cycle;
    private XBotMain xBotMain;
    private PersistantNetwork tcp;

    private InetSocketAddress cand;

    XBotInit(XBotMain xBotMain, PersistantNetwork tcp) {
        this.cycle = xBotMain.id();
        this.xBotMain = xBotMain;
        this.tcp = tcp;

        cand = null;
    }

    void optimize() {
        if(xBotMain.isBiasedActiveViewEmpty())
            return;

        cand = xBotMain.chooseCand();

        try {
            xBotMain.getCost(cand, this);
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
            if(xBotMain.isBiasedActiveViewEmpty())
                return false;

            XBotMain.BiasedInetAddress old = xBotMain.worstBiasedPeer();

            if(!xBotMain.canOptimize(xBotMain.id(), old.address)) {
                xBotMain.finishCycle(cycle);
                return false;
            }

            Message msg = new OptimizationMessage(xBotMain.id(), old.address, old.cost, cost);

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
