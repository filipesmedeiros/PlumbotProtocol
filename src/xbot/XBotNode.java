package xbot;

import messages.Message;
import messages.xbot.*;
import network.Network;
import plumtree.TreeBroadcast;

import java.net.InetSocketAddress;
import java.util.*;

// TODO Drop peers (from the cost map) periodically or maybe after a certain limit

public class XBotNode implements PeerSampling {

    private Map<InetSocketAddress, Long> peers; // with costs
    private SortedSet<InetSocketAddress> activeView;

    private Set<InetSocketAddress> passiveView;

    private Set<InetSocketAddress> waits; // TODO

    private Oracle oracle; // TODO

    private final int activeViewSize;
    private final int passiveViewSize;
    private final int numUnbiasedPeers;
    private final int passiveScanLength;
    private final int arwl;
    private final int prwl;

    private Network network;

    private TreeBroadcast treeBroadcast;

    private InetSocketAddress id;

    public XBotNode(InetSocketAddress id, Oracle oracle, int fanout, int k, int numUnbiasedPeers, int passiveScanLength,
                    int arwl, int prwl, TreeBroadcast treeBroadcast) {
        peers = new HashMap<>();
        activeView = new TreeSet<>((peer1, peer2) -> {
            long diff = peers.get(peer1) - peers.get(peer2);
            return diff > 0 ? 1 : (diff == 0 ? 0 : -1);
        });

        passiveView = new HashSet<>();

        waits = new HashSet<>();

        this.oracle = oracle;

        activeViewSize = fanout + 1;
        passiveViewSize = activeViewSize * k;
        this.numUnbiasedPeers = numUnbiasedPeers;
        this.passiveScanLength = passiveScanLength;
        this.arwl = arwl;
        this.prwl = prwl;

        this.treeBroadcast = treeBroadcast;

        this.id = id;
    }

    public void join(InetSocketAddress connectPeer) {
        network.send(new JoinMessage(id).serialize(), connectPeer);
    }

    @Override
    public Collection<InetSocketAddress> getPeers() {
        return activeView;
    }

    @Override
    public void deliverMessage(Message m) {
        switch(m.type()) {
            case Join:
                handleJoin((JoinMessage) m);
                break;
            case ForwardJoin:
                handleForwardJoin((ForwardJoinMessage) m);
                break;
            case Optimize:
                handleOptimize((OptimizeMessage) m);
                break;
            case OptimizeReply:
                handleOptimizeReply((OptimizeReplyMessage) m);
                break;
            case Replace:
                handleReplace((ReplaceMessage) m);
                break;
            case ReplaceReply:
                handleReplaceReply((ReplaceReplyMessage) m);
                break;
            case Switch:
                handleSwitch((SwitchMessage) m);
                break;
            case SwitchReply:
                handleSwitchReply((SwitchReplyMessage) m);
                break;
            case Disconnect:
                handleDisconnect((DisconnectMessage) m);
                break;
        }
    }

    private void handleJoin(JoinMessage m) {
        addPeerToActiveView(m.sender());
        for(InetSocketAddress peer : activeView)
            if(!peer.equals(m.sender()))
                network.send(new ForwardJoinMessage(id, arwl, m.sender()).serialize(), peer);
    }

    private void handleForwardJoin(ForwardJoinMessage m) {
        if(m.ttl() == 0 || activeView.size() == 0)
            addPeerToActiveView(m.joiningPeer());
        else {
            if(m.ttl() == prwl)
                addPeerToPassiveView(m.joiningPeer());
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
        if(getCost(m.old()) + m.itoc() >= m.itoo() + getCost(m.sender()))
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

    private void addPeerToActiveView(InetSocketAddress peer) {
        if(!peer.equals(id) && activeView.contains(peer)) {
            if(activeView.size() == activeViewSize)
                dropRandomPeerFromActiveView();
            activeView.add(peer);
        }

        if(!peers.containsKey(peer))
            peers.put(peer, oracle.getCost(peer));

        treeBroadcast.peerUp(peer);
    }

    private void addPeerToPassiveView(InetSocketAddress peer) {
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

    private long getCost(InetSocketAddress peer) {
        if(peers.containsKey(peer))
            return peers.get(peer);
        else {
            long cost = oracle.getCost(peer);
            peers.put(peer, cost);
            return cost;
        }
    }

    private void storeWait(InetSocketAddress peer) {
        waits.add(peer);
    }

    private void endWait(InetSocketAddress peer) {
        waits.remove(peer);
    }

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
}
