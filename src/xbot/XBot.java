package xbot;

import babel.exceptions.DestinationProtocolDoesNotExist;
import babel.exceptions.HandlerRegistrationException;
import babel.exceptions.NotificationDoesNotExistException;
import babel.exceptions.ProtocolDoesNotExist;
import babel.notification.ProtocolNotification;
import babel.protocol.GenericProtocol;
import babel.protocol.event.ProtocolMessage;
import babel.requestreply.ProtocolRequest;
import babel.timer.ProtocolTimer;
import network.Host;
import network.INetwork;
import network.INodeListener;
import plumtree.notifications.PeerDown;
import plumtree.notifications.PeerUp;
import xbot.messages.*;
import xbot.oracle.notifications.CostNotification;
import xbot.oracle.requests.CostRequest;
import xbot.requests.GetPeersReply;
import xbot.requests.GetPeersRequest;
import xbot.timers.OptimizationTimer;
import xbot.timers.WaitTimeout;

import java.net.InetAddress;
import java.util.*;
import java.util.function.Consumer;

// TODO Drop peers (from the cost map) periodically or maybe after a certain limit

public class XBot extends GenericProtocol implements INodeListener {

    public static final short PROTOCOL_CODE = 200;
    public static final String PROTOCOL_NAME = "X-Bot";

    private SortedSet<CostedHost> activeView; // To order the active view by cost
    private Map<Host, Long> activeViewPeers; // To have easy access to costs, when given a Host

    private Map<Host, Consumer<CostNotification>> costsWaitingCallbacks;

    private Map<Host, Long> passiveView;

    private Set<Host> waits; // TODO

    private int activeViewSize;
    private int passiveViewSize;
    private long optimizationPeriod;
    private int numUnbiasedPeers;
    private int passiveScanLength;
    private int arwl;
    private int prwl;
    private long threshold;
    private long waitTimeout;

    public XBot(INetwork net)
            throws HandlerRegistrationException, NotificationDoesNotExistException, ProtocolDoesNotExist {

        super(PROTOCOL_NAME, PROTOCOL_CODE, net);

        registerMessageHandler(DisconnectMessage.MSG_CODE, this::handleDisconnect, DisconnectMessage.serializer);
        registerMessageHandler(ForwardJoinMessage.MSG_CODE, this::handleForwardJoin, ForwardJoinMessage.serializer);
        registerMessageHandler(JoinMessage.MSG_CODE, this::handleJoin, JoinMessage.serializer);
        registerMessageHandler(OptimizeMessage.MSG_CODE, this::handleOptimize, OptimizeMessage.serializer);
        registerMessageHandler(OptimizeReplyMessage.MSG_CODE,
                this::handleOptimizeReply, OptimizeReplyMessage.serializer);
        registerMessageHandler(ReplaceMessage.MSG_CODE, this::handleReplace, ReplaceMessage.serializer);
        registerMessageHandler(ReplaceReplyMessage.MSG_CODE, this::handleReplaceReply, ReplaceReplyMessage.serializer);
        registerMessageHandler(SwitchMessage.MSG_CODE, this::handleSwitch, SwitchMessage.serializer);
        registerMessageHandler(SwitchReplyMessage.MSG_CODE, this::handleSwitchReply, SwitchReplyMessage.serializer);

        registerTimerHandler(OptimizationTimer.TIMER_CODE, this::handleOptimizationTimer);
        registerTimerHandler(WaitTimeout.TIMER_CODE, this::handleWaitTimeout);

        registerNotificationHandler(PROTOCOL_CODE, CostNotification.NOTIFICATION_CODE, this::handleCostNotification);

        registerRequestHandler(GetPeersRequest.REQUEST_CODE, this::handleGetPeersRequest);

        registerNodeListener(this);
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
    // Version calculates ctod here, so it's easier to maintain state on nodes (1 cost calc per node)
    private void handleOptimize(ProtocolMessage m) {
        if(!(m instanceof OptimizeMessage))
            return;

        OptimizeMessage message = (OptimizeMessage) m;

        if(availableSlotsInActiveView() > 0) {
            addPeerToActiveView(message.getFrom());

            OptimizeReplyMessage optimizeReplyMessage = new OptimizeReplyMessage()
                    .setOld(message.old())
                    .setAnswer(true)
                    .setHasDisconnect(false)
                    .setDisconnect(null);

            sendMessage(optimizeReplyMessage, message.getFrom());
        } else {
            Host disconnect = activeView.last().host;
            requestCost(disconnect, costNotification -> finishHandleOptimize(costNotification, message));
        }
    }

    private void finishHandleOptimize(CostNotification n, OptimizeMessage m) {
        ReplaceMessage replaceMessage = new ReplaceMessage()
                .setInitiator(m.getFrom())
                .setOld(m.old())
                .setItoc(m.itoc())
                .setItoo(m.itoo())
                .setCtod(n.cost());

        sendMessage(replaceMessage, n.peer());
    }

    private void handleOptimizeReply(ProtocolMessage m) {
        if(!(m instanceof OptimizeReplyMessage))
            return;

        OptimizeReplyMessage message = (OptimizeReplyMessage) m;

        if(message.answer()) {
            if(activeViewPeers.containsKey(message.old())) {
                DisconnectMessage disconnectMessage = new DisconnectMessage()
                        .setWait(message.hasDisconnect());
                sendMessage(disconnectMessage, message.old());
            }
            switchPeerFromPassiveToActive(message.getFrom(), message.itoc());
        }
    }

    private void handleReplace(ProtocolMessage m) {
        if(!(m instanceof ReplaceMessage))
            return;

        ReplaceMessage message = (ReplaceMessage) m;

        requestCost(message.old(), costNotification -> finishHandleReplace(costNotification, message));
    }

    private void finishHandleReplace(CostNotification n, ReplaceMessage m) {
        if(isBetter(m.itoc(), m.itoo(), m.ctod(), n.cost())) {
            SwitchMessage switchMessage =  new SwitchMessage()
                    .setInitiator(m.initiator())
                    .setCandidate(m.getFrom())
                    .setDtoo(n.cost());

            sendMessage(switchMessage, m.old());
        } else {
            ReplaceReplyMessage replaceReplyMessage = new ReplaceReplyMessage()
                    .setAnswer(true)
                    .setInitiator(m.initiator())
                    .setOld(m.old())
                    .setItoc(m.itoc());

            sendMessage(replaceReplyMessage, m.getFrom());
        }
    }

    private void handleReplaceReply(ProtocolMessage m) {
        if(!(m instanceof ReplaceReplyMessage))
            return;

        ReplaceReplyMessage message = (ReplaceReplyMessage) m;

        if(message.answer()) {
            dropPeerFromActiveView(message.getFrom());
            addPeerToActiveView(message.initiator(), message.itoc());

            OptimizeReplyMessage optimizeReplyMessage = new OptimizeReplyMessage()
                    .setAnswer(true)
                    .setOld(message.old())
                    .setHasDisconnect(true)
                    .setDisconnect(message.getFrom());

            sendMessage(optimizeReplyMessage, message.initiator());
        } else {
            // TODO have to send useless host (old and disconnect) bytes so serializer works?
            OptimizeReplyMessage optimizeReplyMessage = new OptimizeReplyMessage()
                    .setAnswer(false)
                    .setOld(message.old())
                    .setHasDisconnect(false)
                    .setDisconnect(message.getFrom());

            sendMessage(optimizeReplyMessage, message.initiator());
        }
    }

    private void handleSwitch(ProtocolMessage m) {
        if(!(m instanceof SwitchMessage))
            return;

        SwitchMessage message = (SwitchMessage) m;

        if(activeViewPeers.containsKey(message.initiator()) || waits.contains(message.initiator())) {
            dropPeerFromActiveView(message.initiator());
            addPeerToActiveView(message.getFrom(), message.dtoo());
            endWait(message.initiator());

            DisconnectMessage disconnectMessage = new DisconnectMessage()
                    .setWait(true);
            sendMessage(disconnectMessage, message.initiator());
        }

        SwitchReplyMessage switchReplyMessage = new SwitchReplyMessage()
                .setAnswer(true)
                .setInitiator(message.initiator())
                .setCandidate(message.candidate());

        sendMessage(switchReplyMessage, message.getFrom());
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

    private void switchPeerFromPassiveToActive(Host peer, long cost) {
        if(passiveView.containsKey(peer) && !activeViewPeers.containsKey(peer)) {
            passiveView.remove(peer);
            activeViewPeers.put(peer, cost);
            addPeerToActiveView(peer);
        }
    }

    private int availableSlotsInActiveView() {
        return activeView.size() - waits.size();
    }

    private void switchPeerFromPassiveToActive(Host peer) {
        switchPeerFromPassiveToActive(peer, -Long.MAX_VALUE);
    }

    private void dropPeerFromActiveView(Host peer) {
        dropPeerFromActiveView(new CostedHost(peer, activeViewPeers.get(peer)));
    }

    private void dropPeerFromActiveView(CostedHost peer) {
        DisconnectMessage disconnectMessage = new DisconnectMessage()
                .setWait(false);
        sendMessage(disconnectMessage, peer.host);

        activeViewPeers.remove(peer.host);
        activeView.remove(peer);
        addPeerToPassiveView(peer.host, peer.cost);

        PeerDown peerDown = new PeerDown(peer.host);
        triggerNotification(peerDown);
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

    private void addPeerToActiveView(Host peer, long cost) {
        if(activeViewPeers.containsKey(peer))
            return;

        if(availableSlotsInActiveView() == 0)
            dropRandomPeerFromActiveView();

        activeView.add(new CostedHost(peer, cost));
        activeViewPeers.put(peer, cost);

        PeerUp peerUp = new PeerUp(peer);
        triggerNotification(peerUp);
    }

    private void addPeerToActiveView(Host peer) {
        if(activeViewPeers.containsKey(peer))
            return;

        if(availableSlotsInActiveView() == 0)
            dropRandomPeerFromActiveView();

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

        PeerUp peerUp = new PeerUp(n.peer());
        triggerNotification(peerUp);
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

        setupTimer(new WaitTimeout(peer), waitTimeout);
    }

    private void endWait(Host peer) {
        waits.remove(peer);
    }

    private void handleWaitTimeout(ProtocolTimer t) {
        if(!(t instanceof WaitTimeout))
            return;

        WaitTimeout timeout = (WaitTimeout) t;
        endWait(timeout.peer());
    }

    private void handleGetPeersRequest(ProtocolRequest r) {
        if(!(r instanceof GetPeersRequest))
            return;

        GetPeersRequest request = (GetPeersRequest) r;

        List<Host> peers = new ArrayList<>(activeViewPeers.keySet());

        GetPeersReply getPeersReply = new GetPeersReply(request.id(), peers);
        getPeersReply.invertDestination(request);
        try {
            sendReply(getPeersReply);
        } catch(DestinationProtocolDoesNotExist dpdne) {
            dpdne.printStackTrace();
            System.exit(1);
        }
    }

    // TODO why only trigger optimizations when cost is better than the old cost? Let the disconnect node decide?
    private void handleOptimizationTimer(ProtocolTimer t) {
        if(availableSlotsInActiveView() == 0) {
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
        activeViewPeers = new HashMap<>();
        costsWaitingCallbacks = new HashMap<>();
        passiveView = new HashMap<>();
        waits = new HashSet<>();

        activeViewSize = Integer.parseInt(properties.getProperty("xbot_fanout", "4")) + 1;
        passiveViewSize = activeViewSize * Integer.parseInt(properties.getProperty("xbot_k", "3"));
        optimizationPeriod = Long.parseLong(properties.getProperty("xbot_optimization_period", "10000"));
        numUnbiasedPeers = Integer.parseInt(properties.getProperty("xbot_unbiased_peers", "1"));
        passiveScanLength = Integer.parseInt(properties.getProperty("xbot_passive_scan_length", "1"));
        arwl = Integer.parseInt(properties.getProperty("xbot_active_random_walk_length", "5"));
        prwl = Integer.parseInt(properties.getProperty("xbot_passive_random_walk_length", "3"));
        threshold = Long.parseLong(properties.getProperty("xbot_threshold", "1000"));
        waitTimeout = Long.parseLong(properties.getProperty("xbot_wait_timeout", "3000"));

        setupPeriodicTimer(new OptimizationTimer(), optimizationPeriod, optimizationPeriod);

        if(properties.containsKey("Contact"))
            try {
                String[] hostElems = properties.getProperty("Contact").split(":");
                Host contact = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));

                JoinMessage joinMessage = new JoinMessage();
                sendMessage(joinMessage, contact);
            } catch(Exception e) {
                System.err.println("Invalid contact on configuration: '" + properties.getProperty("Contact"));
            }
    }

    @Override
    public void nodeDown(Host peer) {
        if(activeViewPeers.containsKey(peer))
            dropPeerFromActiveView(peer);
        passiveView.remove(peer);
    }

    // TODO need to do something here? or just let the new node send the Join Message to start doing stuff?
    // TODO this is just the TCP connection right?
    @Override
    public void nodeUp(Host peer) {
        System.out.println("node up: " + peer);
    }

    @Override
    public void nodeConnectionReestablished(Host peer) {
        System.out.println("connection reestablished: " + peer);
    }

    private static class CostedHost implements Comparable<CostedHost> {

        private Host host;
        private long cost;

        CostedHost(Host host, long cost) {
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
