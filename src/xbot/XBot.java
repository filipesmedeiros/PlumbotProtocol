package xbot;

import babel.exceptions.HandlerRegistrationException;
import babel.protocol.GenericProtocol;
import babel.protocol.event.ProtocolMessage;
import network.Host;
import network.INetwork;
import xbot.messages.ForwardJoinMessage;
import xbot.messages.JoinMessage;
import xbot.oracle.notifications.CostNotification;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

// TODO Drop peers (from the cost map) periodically or maybe after a certain limit

public class XBot extends GenericProtocol {

    public static final short PROTOCOL_CODE = 200;
    public static final String PROTOCOL_NAME = "X-Bot";

    private SortedSet<CostedHost> activeView;

    private Map<Host, Long> passiveView;

    private Set<InetSocketAddress> waits; // TODO

    private int activeViewSize;
    private int passiveViewSize;
    private int numUnbiasedPeers;
    private int passiveScanLength;
    private int arwl;
    private int prwl;
    private int threshold;

    public XBot(INetwork net) throws HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_CODE, net);

        registerMessageHandler(JoinMessage.MSG_CODE, this::handleJoin, JoinMessage.serializer);
    }

    public void join(InetSocketAddress connectPeer) {
        network.send(new JoinMessage(id).serialize(), connectPeer);
    }

    private void handleJoin(ProtocolMessage m) {
        if(!(m instanceof JoinMessage))
            return;

        JoinMessage message = (JoinMessage) m;

        addPeerToActiveView(message.getFrom());
        for(CostedHost peer : activeView)
            if(!peer.host.equals(message.getFrom())) {
                ForwardJoinMessage forwardJoinMessage = new ForwardJoinMessage();
                forwardJoinMessage.setTtl(arwl);
                forwardJoinMessage.setJoiningPeer(message.getFrom());
                sendMessage(forwardJoinMessage, peer.host);
            }
    }

    private void handleForwardJoin(ProtocolMessage m) {
        if(!(m instanceof ForwardJoinMessage))
            return;

        ForwardJoinMessage message = (ForwardJoinMessage) m;

        if(message.ttl() == 0 || activeView.size() == 0)
            addPeerToActiveView(message.joiningPeer());
        else {
            if(message.ttl() == prwl)
                addPeerToPassiveView(message.joiningPeer());
            network.send(new ForwardJoinMessage(id, m).serialize(), selectRandomPeerFromActiveView());
        }
    }

    private void handleOptimize(OptimizeMessage m) {
        if(activeView.size() != activeViewSize) {
            addPeerToActiveView(m.sender());
            network.send(new OptimizeReplyMessage(id, m.old(), true, false, null).serialize(),
                    m.sender());
        } else
            network.send(new ReplaceMessage(id, m.sender(), m.old(), m.itoc(), m.itoo()).serialize(),
                    activeView.last());
    }

    private void handleOptimizeReply(OptimizeReplyMessage m) {
        if(m.answer()) {
            if(activeView.contains(m.old()))
                if(m.hasDisconnect())
                    network.send(new DisconnectMessage(id, true).serialize(), m.old());
                else
                    network.send(new DisconnectMessage(id, false).serialize(), m.old());
            switchPeerFromActiveToPassive(m.sender());
        }
    }

    private void handleReplace(ReplaceMessage m) {
        if(isBetter(m.itoc(), m.itoo(), getCost(m.sender()), getCost(m.old())))
            network.send(new SwitchMessage(id, m.initiator(), m.sender(), getCost(m.old())).serialize(), m.old());
        else
            network.send(new ReplaceReplyMessage(id, true, m.initiator(), m.old()).serialize(), m.sender());
    }

    private void handleReplaceReply(ReplaceReplyMessage m) {
        if(m.answer()) {
            dropPeerFromActiveView(m.sender());
            addPeerToActiveView(m.initiator());
            network.send(new OptimizeReplyMessage(id, m.old(), true, true, m.sender()).serialize(),
                    m.initiator());
        } else
            network.send(new OptimizeReplyMessage(id, null, false, false, null).serialize(),
                    m.initiator());
    }

    private void handleSwitch(SwitchMessage m) {
        if(activeView.contains(m.initiator()) || waits.contains(m.initiator())) {
            network.send(new DisconnectMessage(id, true).serialize(), m.initiator());
            dropPeerFromActiveView(m.initiator());
            addPeerToActiveView(m.sender());
            endWait(m.initiator());
        }
        network.send(new SwitchReplyMessage(id, true, m.initiator(), m.candidate()).serialize(), m.sender());
    }

    private void handleSwitchReply(SwitchReplyMessage m) {
        if(m.answer()) {
            dropPeerFromActiveView(m.candidate());
            addPeerToActiveView(m.sender());
        }
        network.send(new ReplaceReplyMessage(id, m.answer(), m.initiator(), m.sender()).serialize(), m.sender());
    }

    private void handleDisconnect(DisconnectMessage m) {
        if(m.isWait())
            storeWait(m.sender());
        switchPeerFromActiveToPassive(m.sender());
    }

    private void switchPeerFromActiveToPassive(InetSocketAddress peer) {
        if(!peer.equals(id) && activeView.contains(peer) && !passiveView.contains(peer)) {
            dropPeerFromActiveView(peer);
            addPeerToPassiveView(peer);
        }
    }

    private void switchPeerFromPassiveToActive(InetSocketAddress peer) {
        if(!peer.equals(id) && passiveView.contains(peer) && !activeView.contains(peer)) {
            passiveView.remove(peer);
            addPeerToActiveView(peer);
        }
    }

    private void dropPeerFromActiveView(InetSocketAddress peer) {
        activeView.remove(peer);
        treeBroadcast.peerDown(peer);
    }

    private void dropRandomPeerFromActiveView() {
        InetSocketAddress removedPeer = selectRandomPeerFromActiveView();

        network.send(new DisconnectMessage(id, false).serialize(), removedPeer);

        activeView.remove(removedPeer);
        passiveView.add(removedPeer);
    }

    private void dropRandomPeerFromPassiveView() {
        InetSocketAddress[] array = passiveView.toArray(new InetSocketAddress[0]);
        InetSocketAddress removedPeer = array[new Random().nextInt(array.length)];
        passiveView.remove(removedPeer);
    }

    private void addPeerToActiveView(Host peer) {
        if(!peer.equals(id) && activeView.contains(peer)) {
            if(activeView.size() == activeViewSize)
                dropRandomPeerFromActiveView();
            activeView.add(peer);
        }

        if(!peers.containsKey(peer))
            peers.put(peer, oracle.getCost(peer));

        treeBroadcast.peerUp(peer);
    }

    private void addPeerToPassiveView(Host peer) {
        if(!peer.equals(id) && !activeView.contains(peer) && passiveView.contains(peer)) {
            if(passiveView.size() == passiveViewSize)
                dropRandomPeerFromPassiveView();
            passiveView.add(peer);
        }

        if(!peers.containsKey(peer))
            peers.put(peer, oracle.getCost(peer));
    }

    private InetSocketAddress selectRandomPeerFromActiveView() {
        InetSocketAddress[] array = activeView.toArray(new InetSocketAddress[0]);
        return array[new Random().nextInt(array.length)];
    }

    // TODO optimize this
    private List<InetSocketAddress> selectRandomPeersFromPassiveView(int num) {
        ArrayList<InetSocketAddress> list =
                new ArrayList<>(Arrays.asList(activeView.toArray(new InetSocketAddress[0])));
        Random r = new Random();
        while(list.size() > num)
            list.remove(r.nextInt(list.size()));
        list.trimToSize();
        return list;
    }

    private Future<Long> getCost(InetSocketAddress peer) {
        if(peers.containsKey(peer))
            return CompletableFuture.completedFuture(peers.get(peer));
        else
            return oracle.getCost(peer);
    }

    private void storeWait(InetSocketAddress peer) {
        waits.add(peer);
    }

    private void endWait(InetSocketAddress peer) {
        waits.remove(peer);
    }

    // TODO
    public void fireOptimizationTimer() {
        if(activeView.size() == activeViewSize) {
            List<InetSocketAddress> candidates = selectRandomPeersFromPassiveView(passiveScanLength);
            Iterator<InetSocketAddress> olds = activeView.iterator();
            int index = 0;
            while(olds.hasNext()) {
                if(index < numUnbiasedPeers) {
                    olds.next();
                    index++;
                } else
                    break;
            }
            olds.forEachRemaining(old -> {
                while(candidates.size() != 0) {
                    InetSocketAddress candidate = candidates.remove(0);
                    if(getCost(candidate) < getCost(old)) {
                        network.send(new OptimizeMessage(id, old, getCost(candidate), getCost(old)).serialize(),
                                candidate);
                        break;
                    }
                }
            });
        }
    }

    private boolean isBetter(long itoc, long itoo, long ctod, long dtoo) {
        return itoc + dtoo + threshold < itoo + ctod;
    }

    @Override
    public void init(Properties properties) {
        activeView = new TreeSet<>();

    }

    private static class CostedHost implements Comparable<CostedHost> {

        private Host host;
        private long cost;

        public CostedHost(Host host, long cost) {
            this.host = host;
            this.cost = cost;
        }

        @Override
        public int compareTo(CostedHost o) {
            return Math.toIntExact(cost - o.cost);
        }
    }
}
