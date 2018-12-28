package protocol.rework;

import message.Message;
import message.xbot.*;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

public class XBotCand {

    private InetSocketAddress cycle;
    private XBotMain xBotMain;
    private PersistantNetwork tcp;

    private InetSocketAddress init;
    private long initToCand;

    XBotCand(InetSocketAddress cycle, XBotMain xBotMain, PersistantNetwork tcp) {
        this.cycle = cycle;
        this.xBotMain = xBotMain;
        this.tcp = tcp;

        init = null;
        initToCand = 0;
    }

    void handleOptimization(OptimizationMessage optimizationMessage) {
        if(xBotMain.isBiasedActiveViewEmpty())
            try {
                Message reply = new OptimizationReplyMessage(xBotMain.id(), false, false);
                tcp.send(reply.bytes(), optimizationMessage.sender());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        else
            try {
                init = optimizationMessage.sender();
                initToCand = optimizationMessage.itoc();

                InetSocketAddress old = optimizationMessage.old();
                long initToOld = optimizationMessage.itoo();

                Message replace = new ReplaceMessage(xBotMain.id(), init, old, initToOld, initToCand);
                tcp.send(replace.bytes(), xBotMain.worstBiasedPeer().address);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    void handleReplaceReply(ReplaceReplyMessage replaceReplyMessage) {
        boolean removed = false;

        if(replaceReplyMessage.accept()) {
            removed = xBotMain.removeFromBiased(replaceReplyMessage.sender());

            xBotMain.addPeerToBiasedActiveView(init, initToCand);
        }

        try {
            Message optimizationReply = new OptimizationReplyMessage(xBotMain.id(), replaceReplyMessage.accept(), removed);

            tcp.send(optimizationReply.bytes(), init);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }
}
