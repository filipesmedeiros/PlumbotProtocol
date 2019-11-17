package xbot;

import babel.exceptions.DestinationProtocolDoesNotExist;
import babel.exceptions.HandlerRegistrationException;
import babel.exceptions.NotificationDoesNotExistException;
import babel.exceptions.ProtocolDoesNotExist;
import babel.notification.ProtocolNotification;
import babel.protocol.GenericProtocol;
import babel.protocol.event.ProtocolMessage;
import network.Host;
import network.INetwork;
import plumtree.notifications.PeerUp;
import xbot.messages.*;
import xbot.oracle.notifications.CostNotification;
import xbot.oracle.requests.CostRequest;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;

// TODO Drop peers (from the cost map) periodically or maybe after a certain limit

public class XBot extends GenericProtocol {

    public static final short PROTOCOL_CODE = 200;
    public static final String PROTOCOL_NAME = "X-Bot";

    private SortedSet<CostedHost> activeView; // To order the active view by cost
    private Map<Host, Long> activeViewPeers; // To have easy access to costs, when given a Host

    private Map<Host, Consumer<CostNotification>> costsWaitingCallbacks;

    private Map<Host, Long> passiveView;

    private Set<Host> waits; // TODO

    private int activeViewSize;
    private int passiveViewSize;
    private int numUnbiasedPeers;
    private int passiveScanLength;
    private int arwl;
    private int prwl;
    private int threshold;

    public XBot(INetwork net)
            throws HandlerRegistrationException, NotificationDoesNotExistException, ProtocolDoesNotExist {

        super(PROTOCOL_NAME, PROTOCOL_CODE, net);

        registerMessageHandler(JoinMessage.MSG_CODE, this::handleJoin, JoinMessage.serializer);
        registerMessageHandler(ForwardJoinMessage.MSG_CODE, this::handleForwardJoin, ForwardJoinMessage.serializer);

        registerNotificationHandler(PROTOCOL_CODE, CostNotification.NOTIFICATION_CODE, this::handleCostNotification);
    }

    private void handleCostNotification(ProtocolNotification n) {
        if(!(n instanceof CostNotification))
            return;

        CostNotification notification = (CostNotification) n;

        Consumer<CostNotification> handler = costsWaitingCallbacks.get(notification.peer());
        if(handler == null)
            return;

        handler.accept(notification);

        costsWaitingCallbacks.remove(notification.peer());
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
            ForwardJoinMessage forwardJoinMessage = new ForwardJoinMessage()
                    .setJoiningPeer(message.joiningPeer())
                    .setTtl(message.ttl() - 1);
            sendMessage(forwardJoinMessage, selectRandomPeerFromActiveView());
        }
    }

    // Version that assumes itoc is already in the message
    private void handleOptimize(ProtocolMessage m) {
        if(!(m instanceof OptimizeMessage))
            return;

        OptimizeMessage message = (OptimizeMessage) m;

        if(activeView.size() != activeViewSize) {
            addPeerToActiveView(message.getFrom());

            OptimizeReplyMessage optimizeReplyMessage = new OptimizeReplyMessage()
                    .setOld(message.old())
                    .setAnswer(true)
                    .setHasDisconnect(false)
                    .setDisconnect(null);

            sendMessage(optimizeReplyMessage, message.getFrom());
        } else {
            ReplaceMessage replaceMessage = new ReplaceMessage()
                    .setInitiator(message.getFrom())
                    .setOld(message.old())
                    .setItoc(message.itoc())
                    .setItoo(message.itoo());

            sendMessage(replaceMessage, activeView.last().host);
        }
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

    private void handleSwitchReply(ProtocolMessage m) {
        if(!(m instanceof SwitchReplyMessage))
            return;

        SwitchReplyMessage message = (SwitchReplyMessage) m;

        if(message.answer()) {
            dropPeerFromActiveView(message.candidate());
            addPeerToActiveView(message.getFrom());
        }

        ReplaceReplyMessage replaceReplyMessage = new ReplaceReplyMessage()
                .setAnswer(message.answer())
                .setInitiator(message.initiator())
                .setOld(message.getFrom());
        sendMessage(replaceReplyMessage, message.candidate());
    }

    private void handleDisconnect(ProtocolMessage m) {
        if(!(m instanceof DisconnectMessage))
            return;

        DisconnectMessage message = (DisconnectMessage) m;

        if(message.isWait())
            storeWait(message.getFrom());
        dropPeerFromActiveView(m.getFrom());
    }

    private void switchPeerFromPassiveToActive(Host peer) {
        if(passiveView.containsKey(peer) && !activeViewPeers.containsKey(peer)) {
            passiveView.remove(peer);
            activeViewPeers.put(peer, -Long.MAX_VALUE);
            addPeerToActiveView(peer);
        }
    }

    private void dropPeerFromActiveView(Host peer) {
        for(CostedHost costedPeer : activeView)
            if(costedPeer.host.equals(peer))
                dropPeerFromActiveView(costedPeer);
    }

    private void dropPeerFromActiveView(CostedHost peer) {
        DisconnectMessage disconnectMessage = new DisconnectMessage()
                .setWait(false);
        sendMessage(disconnectMessage, peer.host);

        activeViewPeers.remove(peer.host);
        activeView.remove(peer);
        addPeerToPassiveView(peer.host, peer.cost);
    }

    private void dropRandomPeerFromActiveView() {
        CostedHost removedPeer = selectRandomPeerWithCostFromActiveView();
        dropPeerFromActiveView(removedPeer);
    }

    private void dropRandomPeerFromPassiveView() {
        Host[] array = passiveView.keySet().toArray(new Host[0]);
        Host removedPeer = array[new Random().nextInt(array.length)];
        passiveView.remove(removedPeer);
    }

    private void addPeerToActiveView(Host peer) {
        if(activeViewPeers.containsKey(peer))
            return;

        if(activeView.size() == activeViewSize) {
            dropRandomPeerFromActiveView();
        }

        activeViewPeers.put(peer, -Long.MAX_VALUE);

        PeerUp peerUp = new PeerUp(peer);
        triggerNotification(peerUp);

        costsWaitingCallbacks.put(peer, this::finishAddPeerToActiveView);
    }

    private void finishAddPeerToActiveView(CostNotification n) {
        if(!activeViewPeers.containsKey(n.peer()))
            return;

        activeView.add(new CostedHost(n.peer(), n.cost()));
        activeViewPeers.put(n.peer(), n.cost());
    }

    private void addPeerToPassiveView(Host peer, long cost) {
        if(!activeViewPeers.containsKey(peer) && !passiveView.containsKey(peer)) {
            if(passiveView.size() >= passiveViewSize)
                dropRandomPeerFromPassiveView();
            passiveView.put(peer, cost);
        }
    }

    private void addPeerToPassiveView(Host peer) {
        addPeerToPassiveView(peer, -Long.MAX_VALUE);
    }

    // TODO Optimize?
    private Host selectRandomPeerFromActiveView() {
        Host[] array = activeViewPeers.keySet().toArray(new Host[0]);
        return array[new Random().nextInt(array.length)];
    }

    // TODO Optimize?
    private CostedHost selectRandomPeerWithCostFromActiveView() {
        CostedHost[] array = activeView.toArray(new CostedHost[0]);
        return array[new Random().nextInt(array.length)];
    }

    // TODO Optimize?
    private List<Host> selectRandomPeersFromPassiveView(int num) {
        ArrayList<Host> list = new ArrayList<>(Arrays.asList(activeViewPeers.keySet().toArray(new Host[0])));
        Random r = new Random();
        while(list.size() > num)
            list.remove(r.nextInt(list.size()));
        list.trimToSize();
        return list;
    }

    private void storeWait(Host peer) {
        waits.add(peer);
    }

    private void endWait(Host peer) {
        waits.remove(peer);
    }

    // TODO why only trigger optimizations when cost is better than the old cost? Let the disconnect node decide?
    public void optimizationTimerHandler() {
        if(activeView.size() == activeViewSize) {
            List<Host> candidates = selectRandomPeersFromPassiveView(passiveScanLength);
            Iterator<CostedHost> olds = activeView.iterator();
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
                    Host candidate = candidates.remove(0);
                    requestCost(candidate, costNotification -> finishOptimizationTimerHandler(costNotification, old));
                }
            });
        }
    }

    // TODO how to break cycle here? according to spec, if we find an old thats better than cand, we stop, but with
    // TODO async behaviour that's hard
    private void finishOptimizationTimerHandler(CostNotification n, CostedHost old) {
        if(n.cost() < old.cost) {
            OptimizeMessage optimizeMessage = new OptimizeMessage()
                    .setOld(old.host)
                    .setItoc(n.cost())
                    .setItoo(old.cost);
            sendMessage(optimizeMessage, n.peer());
        }
    }

    private void requestCost(Host peer, Consumer<CostNotification> costHandler) {
        CostRequest costRequest = new CostRequest(peer);
        try {
            sendRequest(costRequest);
        } catch (DestinationProtocolDoesNotExist dpdne) {
            dpdne.printStackTrace();
            System.exit(1);
        }
        costsWaitingCallbacks.put(peer, costHandler);
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

        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;

            if(!(obj instanceof CostedHost))
                return false;

            CostedHost other = (CostedHost) obj;
            return this.host.equals(other.host) && this.cost == other.cost;
        }
    }
}
