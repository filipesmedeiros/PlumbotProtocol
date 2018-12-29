package protocol.xbot;

import message.Message;
import message.xbot.*;
import network.PersistantNetwork;

import java.io.IOException;
import java.net.InetSocketAddress;

class XBotCand implements XBotSupport {

    private InetSocketAddress cycle;
    private XBotNode xBotNode;
    private PersistantNetwork tcp;

    private InetSocketAddress init;
    private long initToCand;

    XBotCand(InetSocketAddress cycle, XBotNode xBotNode, PersistantNetwork tcp) {
        this.cycle = cycle;
        this.xBotNode = xBotNode;
        this.tcp = tcp;

        init = null;
        initToCand = 0;
    }

    @Override
    public InetSocketAddress cycle() {
        return cycle;
    }

    void handleOptimization(OptimizationMessage optimizationMessage) {
        if(xBotNode.isBiasedActiveViewEmpty())
            try {
                Message reply = new OptimizationReplyMessage(xBotNode.id(), false, false);
                tcp.send(reply.bytes(), optimizationMessage.sender());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        else if(xBotNode.isntActiveViewFull())
            try {
                Message reply = new OptimizationReplyMessage(xBotNode.id(), true, false);
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

                Message replace = new ReplaceMessage(xBotNode.id(), init, old, initToOld, initToCand);
                tcp.send(replace.bytes(), xBotNode.worstBiasedPeer().address);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    void handleReplaceReply(ReplaceReplyMessage replaceReplyMessage) {
        boolean removed = false;

        if(replaceReplyMessage.accept()) {
            removed = xBotNode.movePeerToPassiveView(replaceReplyMessage.sender());

            xBotNode.movePeerToActiveView(init, initToCand);
        }

        try {
            Message optimizationReply = new OptimizationReplyMessage(xBotNode.id(), replaceReplyMessage.accept(), removed);

            tcp.send(optimizationReply.bytes(), init);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }
}
