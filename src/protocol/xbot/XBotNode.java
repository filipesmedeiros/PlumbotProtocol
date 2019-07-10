package protocol.xbot;

import common.MappedTimerManager;
import common.RandomChooser;
import common.TimerManager;
import exceptions.NotReadyForInitException;
import interfaces.NeighbourhoodListener;
import interfaces.OptimizerNode;
import message.Message;
import message.xbot.*;
import network.NetworkInterface;
import network.PersistantNetwork;
import notifications.CostNotification;
import notifications.MessageNotification;
import notifications.Notification;
import notifications.TCPConnectionNotification;
import protocol.oracle.Oracle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class XBotNode implements OptimizerNode {

    private class ConnectionInfo {

        private long cost;
        private boolean active;
        private XBotSupport xBotSupport;

        private ConnectionInfo(long cost, boolean active, XBotNode xBotNode,
                               TimerManager timerManager, InetSocketAddress peer,
                               XBotSupport xBotSupport) {
            this.cost = cost;
            this.active = active;
            this.xBotSupport = xBotSupport;

            timerManager.addAction(WAIT + peer.toString(), xBotNode::fireConnectionTimeout, peer, waitTimeout);
        }
    }

    static class BiasedInetAddress implements Comparable<BiasedInetAddress> {

        InetSocketAddress address;
        long cost;

        private BiasedInetAddress(InetSocketAddress address, long cost) {
            this.address = address;
            this.cost = cost;
        }

        @Override
        public int compareTo(BiasedInetAddress o) {
            if (address.equals(o.address))
                return 0;
            else if (cost - o.cost < 0)
                return -1;
            else
                return 1;
        }
    }

    // Names for the timers (threads)
    static final String WAIT = "W";
    private static final String OPTI = "O";

    final private InetSocketAddress address;

    private boolean joining;

    private Set<InetSocketAddress> unbiasedActiveView;
    private SortedSet<BiasedInetAddress> biasedActiveView;
    private Set<InetSocketAddress> activeView;
    private Set<InetSocketAddress> passiveView;

    private int activeViewMaxSize;
    private int unbiasedViewMaxSize;
    private int passiveViewMaxSize;

    private RandomChooser<InetSocketAddress> random;

    private TimerManager timerManager;

    private Oracle oracle;
    private BlockingQueue<Notification> notifications;
    private long optimizationPeriod;

    private long waitTimeout;

    private Map<InetSocketAddress, ConnectionInfo> futureConnections;

    // All these are for remembering stuff during async optimization
    private Map<InetSocketAddress, XBotSupportEdge> costsWaiting;

    // Lists of "states" being used by this node for optimizations, currently
    // The cycle each one belongs to is identified by its initiator
    private XBotInit init;
    private Map<InetSocketAddress, XBotCand> cands;
    private Map<InetSocketAddress, XBotDisco> discos;
    private Map<InetSocketAddress, XBotOld> olds;

    private int attl;
    private int pttl;

    private PersistantNetwork tcp;

    private Set<NeighbourhoodListener> neighbourhoodListeners;

    // For testing, every node waits a random (relatively low) amount of time
    // when answering pings
    private long pingSleepTime;

    public XBotNode(InetSocketAddress address, int activeViewMaxSize,
                    int unbiasedViewMaxSize, int passiveViewMaxSize,
                    int optimizationPeriod, long waitTimeOut, int attl, int pttl)
            throws IllegalArgumentException {

        if(activeViewMaxSize <= 0 || address == null)
            throw new IllegalArgumentException();

        this.address = address;

        joining = true;

        this.unbiasedViewMaxSize = unbiasedViewMaxSize;
        this.activeViewMaxSize = activeViewMaxSize;
        this.passiveViewMaxSize = passiveViewMaxSize;

        init = null;
        cands = new HashMap<>();
        discos = new HashMap<>();
        olds = new HashMap<>();

        this.attl = attl;
        this.pttl = pttl;

        this.optimizationPeriod = optimizationPeriod;

        this.waitTimeout = waitTimeOut;

        futureConnections = new HashMap<>();

        costsWaiting = new HashMap<>();
        timerManager = new MappedTimerManager();

        biasedActiveView = new TreeSet<>();
        unbiasedActiveView = new HashSet<>();
        activeView = new HashSet<>();
        passiveView = new HashSet<>();

        random = new RandomChooser<>();

        timerManager = new MappedTimerManager();

        notifications = new ArrayBlockingQueue<>(10);

        pingSleepTime = random.integer(3000);

        tcp = null;
        oracle = null;
        neighbourhoodListeners = new HashSet<>();
    }

    @Override
    public void setOracle(Oracle oracle)
            throws IllegalArgumentException {
        if(oracle == null)
            throw new IllegalArgumentException();

        this.oracle = oracle;
    }

    @Override
    public void setPeriod(long period)
            throws IllegalArgumentException {
        if(period <= 0)
            throw new IllegalArgumentException();

        this.optimizationPeriod = period;
    }

    @Override
    public void notify(Notification notification) {
        notifications.add(notification);
    }

    private void handleOptimization(ByteBuffer bytes) {
        OptimizationMessage msg = OptimizationMessage.parse(bytes);

        XBotCand cand = new XBotCand(msg.sender(), this, tcp);
        cand.handleOptimization(msg);

        cands.put(msg.sender(), cand);
    }

    private void handleOptimizationReply(ByteBuffer bytes) {
        OptimizationReplyMessage msg = OptimizationReplyMessage.parse(bytes);

        init.handleOptimizationReply(msg);
    }

    private void handleReplace(ByteBuffer bytes) {
        ReplaceMessage msg = ReplaceMessage.parse(bytes);

        XBotDisco disco = new XBotDisco(msg.init(), this, tcp);
        disco.handleReplace(msg);

        discos.put(msg.init(), disco);
    }

    private void handleReplaceReply(ByteBuffer bytes) {
        ReplaceReplyMessage msg = ReplaceReplyMessage.parse(bytes);

        cands.get(msg.init()).handleReplaceReply(msg);
    }

    private void handlePing(ByteBuffer bytes) {
        PingMessage msg = PingMessage.parse(bytes);

        Message pingBackMsg = new PingBackMessage(address);

        try {
            Thread.sleep(pingSleepTime);

            tcp.send(pingBackMsg.bytes(), msg.sender());
        } catch(InterruptedException | IllegalArgumentException | IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private XBotOld createOld(InetSocketAddress cycle) {
        XBotOld old;
        if(!olds.containsKey(cycle)) {
            old = new XBotOld(cycle, this, timerManager, waitTimeout);
            olds.put(cycle, old);
        } else
            old = olds.get(cycle);

        return old;
    }

    private void handleDisconnect(ByteBuffer bytes) {
        DisconnectMessage msg = DisconnectMessage.parse(bytes);

        InetSocketAddress peer = msg.sender();

        movePeerToPassiveView(peer);

        if(msg.hasToWait()) {
            XBotOld old = createOld(msg.sender());

            // This is putting the init as the future, but it symbolises
            // we are waiting for the corresponding disconnect node,
            // because we don't know its ID
            futureConnections.put(msg.sender(), null);
            old.handleDisconnectWait();
        }
    }

    private void handleSwitch(ByteBuffer bytes) {
        SwitchMessage msg = SwitchMessage.parse(bytes);

        XBotOld old = createOld(msg.sender());
        old.handleSwitch(msg.sender(), msg.dtoo());
    }

    @Override
    public InetSocketAddress id() {
        return address;
    }

    @Override
    public Set<InetSocketAddress> activeView() {
        return activeView;
    }

    @Override
    public Set<InetSocketAddress> passiveView() {
        return passiveView;
    }

    @Override
    public int setUnbiasedSize(int size) throws IllegalArgumentException {
        // TODO
        return 0;
    }

    @Override
    public int setActiveViewSize(int size) throws IllegalArgumentException {
        if(size <= unbiasedViewMaxSize)
            throw new IllegalArgumentException();

        int num = 0;
        while(activeView.size() > size) {
            // TODO guarantees about unbiased peer number after this
            removeRandomFromActiveView();
            num++;
        }

        activeViewMaxSize = size;
        return num;
    }

    @Override
    public int setPassiveViewSize(int size) throws IllegalArgumentException {
        if(size <= 0)
            throw new IllegalArgumentException();

        int num = 0;
        while(passiveView.size() > size) {
            removeRandomFromPassiveView();
            num++;
        }

        passiveViewMaxSize = size;
        return num;
    }

    // TODO does this screw something? something should be checked?
    @Override
    public boolean setNetwork(NetworkInterface tcp)
            throws IllegalArgumentException {

        if(!(tcp instanceof PersistantNetwork))
            throw new IllegalArgumentException();

        this.tcp = (PersistantNetwork) tcp;
        return true;
    }

    @Override
    public boolean setNeighbourhoodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException {
        if(listener == null)
            throw new IllegalArgumentException();

        return neighbourhoodListeners.add(listener);
    }

    @Override
    public boolean setNeighbourhoodListeners(Set<NeighbourhoodListener> listeners)
            throws IllegalArgumentException {
        if(listeners == null || listeners.isEmpty())
            throw new IllegalArgumentException();

        return neighbourhoodListeners.addAll(listeners);
    }

    @Override
    public boolean removeNeighbourboodListener(NeighbourhoodListener listener)
            throws IllegalArgumentException {
        if(listener == null)
            throw new IllegalArgumentException();

        return neighbourhoodListeners.remove(listener);
    }

    @Override
    public void initialize() throws NotReadyForInitException {
        if(tcp == null || oracle == null)
            throw new NotReadyForInitException();

        timerManager.addTimer(OPTI, this::optimize, optimizationPeriod);

        new Thread(() -> oracle.init()).start();

        processNotification();
    }

    @Override
    public void join(InetSocketAddress contact) {
        if(contact == null) return;

        try {
            beginConnection(contact, -1, true, null);
        } catch(IllegalArgumentException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void leave() {
        for(InetSocketAddress peer : activeView)
            try {
                tcp.disconnect(peer);
            } catch(IOException e) {
                // TODO
                e.printStackTrace();
            }

        for(InetSocketAddress peer : passiveView)
            try {
                tcp.disconnect(peer);
            } catch(IOException e) {
                // TODO
                e.printStackTrace();
            }

        System.exit(0);
    }

    private void sendAcceptJoin(InetSocketAddress peer) {
        try {
            tcp.send(new AcceptJoinMessage(address).bytes(), peer);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleJoin(ByteBuffer bytes) {
        JoinMessage msg = JoinMessage.parse(bytes);

        System.out.println("node of join -> " + msg.sender());

        if(addNewPeerToActiveView(msg.sender()))
            sendAcceptJoin(msg.sender());

        try {
            activeView.forEach((peer) -> {
                Message forwardMsg = new ForwardJoinMessage(address, msg.sender(), attl);

                if(!peer.equals(msg.sender())) {
                    try {
                        tcp.send(forwardMsg.bytes(), peer);
                    } catch(IOException | InterruptedException e) {
                        // TODO
                        e.printStackTrace();
                    }
                }
            });
        } catch(IllegalArgumentException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleForwardJoin(ByteBuffer bytes) {
        ForwardJoinMessage msg = ForwardJoinMessage.parse(bytes);

        try {
            if(msg.ttl() == 0 || activeView.size() <= 1)
                addNewPeerToActiveView(msg.joiner());
            else {
                if(msg.ttl() == pttl)
                    addNewPeerToPassiveView(msg.joiner());

                InetSocketAddress peer = random.fromSet(activeView);

                while(peer.equals(msg.sender()) || peer.equals(msg.joiner()))
                    peer = random.fromSet(activeView);

                tcp.send(msg.next(address).bytes(), peer);
            }
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleAcceptJoin(ByteBuffer bytes) {
        AcceptJoinMessage msg = AcceptJoinMessage.parse(bytes);
        addNewPeerToActiveView(msg.sender());
    }

    private void handleConnection(Notification notification) {
        if(!(notification instanceof TCPConnectionNotification)) {
            System.out.println("??? Wrong connection notification");
            return;
        }

        TCPConnectionNotification tcpNoti = (TCPConnectionNotification) notification;

        finishConnection(tcpNoti.peer());

        // We just joined the network (this is such an ugly way but wtv...)
        if(joining)
            try {
                Message joinMsg = new JoinMessage(address);
                tcp.send(joinMsg.bytes(), tcpNoti.peer());

                joining = false;
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    @SuppressWarnings("all")
    private void handleMessage(Notification notification) {
        if(!(notification instanceof MessageNotification)) {
            System.out.println("??? Wrong message notification");
            return;
        }

        MessageNotification msgNoti = (MessageNotification) notification;

        short type = msgNoti.msgType();

        ByteBuffer msg = msgNoti.message();

        switch(type) {
            case JoinMessage.TYPE:
                handleJoin(msg);
                break;
            case AcceptJoinMessage.TYPE:
                handleAcceptJoin(msg);
                break;
            case ForwardJoinMessage.TYPE:
                handleForwardJoin(msg);
                break;
            case OptimizationMessage.TYPE:
                handleOptimization(msg);
                break;
            case OptimizationReplyMessage.TYPE:
                handleOptimizationReply(msg);
                break;
            case ReplaceMessage.TYPE:
                handleReplace(msg);
                break;
            case ReplaceReplyMessage.TYPE:
                handleReplaceReply(msg);
                break;
            case PingMessage.TYPE:
                handlePing(msg);
                break;
            case DisconnectMessage.TYPE:
                handleDisconnect(msg);
                break;
            case SwitchMessage.TYPE:
                handleSwitch(msg);
                break;

            default:
                System.out.println("??? unrecognized message");
                break;
        }
    }

    private void handleCost(Notification notification) {
        if(!(notification instanceof CostNotification)) {
            System.out.println("??? Wrong cost notification");
            return;
        }

        CostNotification costNoti = (CostNotification) notification;
        InetSocketAddress sender = costNoti.sender();
        long cost = costNoti.cost();

        if(costsWaiting.containsKey(sender)) {
            XBotSupportEdge xBotSupportEdge = costsWaiting.get(sender);

            if(xBotSupportEdge == null)
                addNewPeerToActiveView(sender, cost);
            else
                xBotSupportEdge.handleCost(sender, cost);
        }
    }

    @SuppressWarnings("all")
    private void processNotification() {
        while(true)
            try {
                Notification notification = notifications.take();

                short type = notification.type();

                switch (type) {
                    case TCPConnectionNotification.TYPE:
                        handleConnection(notification);
                        break;
                    case MessageNotification.TYPE:
                        handleMessage(notification);
                        break;
                    case CostNotification.TYPE:
                        handleCost(notification);
                        break;

                    default:
                        System.out.println("??? Wrong notification");
                        break;
                }
            } catch(InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private void optimize() {
        if(init != null)
            return;

        init = new XBotInit(this, tcp);

        if(!init.optimize())
            init = null;
    }

    // Is there a way to know where the peer is coming from
    // so we don't have to search both active views?
    boolean movePeerToPassiveView(InetSocketAddress peer) {
        if(!activeView.remove(peer))
            return false;

        for(BiasedInetAddress biasedInetAddress : biasedActiveView)
            if(biasedInetAddress.address.equals(peer)) {
                biasedActiveView.remove(biasedInetAddress);
                break;
            }
        unbiasedActiveView.remove(peer);

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourDown(peer);

        passiveView.add(peer);

        Message discoMsg = new DisconnectMessage(address, false);
        try {
            tcp.send(discoMsg.bytes(), peer);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }

        return true;
    }

    void movePeerToActiveView(InetSocketAddress peer) {
        if(!passiveView.remove(peer))
            return;

        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourUp(peer);
        } else
            addPeerToBiasedActiveView(peer);
    }

    void movePeerToActiveView(InetSocketAddress peer, long cost) {
        if(!passiveView.remove(peer))
            return;

        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourUp(peer);
        } else
            addPeerToBiasedActiveView(peer, cost);
    }

    void beginConnection(InetSocketAddress peer, long cost, boolean active, XBotSupport xBotSupport) {
        futureConnections.put(peer, new ConnectionInfo(cost, active, this, timerManager, peer, xBotSupport));

        try {
            tcp.connect(peer);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void finishConnection(InetSocketAddress peer) {
        ConnectionInfo info = futureConnections.remove(peer);

        if(info == null)
            return;

        timerManager.stop(WAIT + peer.toString());

        XBotSupport xBotSupport = info.xBotSupport;

        if(xBotSupport != null) {
            XBotDisco disco = (XBotDisco) xBotSupport;
            disco.handleConnectionToOld();
        }

        if(!info.active)
            addNewPeerToPassiveView(peer);
        else
            addNewPeerToActiveView(peer, info);
    }

    private void addPeerToBiasedActiveView(InetSocketAddress peer, long cost) {
        if(activeView.size() == activeViewMaxSize - futureConnections.size())
            movePeerToPassiveView(biasedActiveView.last().address);

        biasedActiveView.add(new BiasedInetAddress(peer, cost));
        activeView.add(peer);
    }

    private void addPeerToBiasedActiveView(InetSocketAddress peer) {
        if(activeView.size() == activeViewMaxSize - futureConnections.size())
            movePeerToPassiveView(biasedActiveView.last().address);

        try {
            getCost(peer, null);
            costsWaiting.put(peer, null);
        } catch (IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private boolean addNewPeerToActiveView(InetSocketAddress peer) {
        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);
            return true;
        } else
            addPeerToBiasedActiveView(peer);

        return false;
    }

    void addNewPeerToActiveView(InetSocketAddress peer, long cost) {
        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);
        } else
            addPeerToBiasedActiveView(peer, cost);
    }

    private void addNewPeerToActiveView(InetSocketAddress peer, ConnectionInfo info) {
        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);
        } else {
            if(activeView.size() == activeViewMaxSize - futureConnections.size())
                movePeerToPassiveView(biasedActiveView.last().address);

            if(info.cost < 0)
                try {
                    getCost(peer, null);
                    costsWaiting.put(peer, null);
                } catch (IOException | InterruptedException e) {
                    // TODO
                    e.printStackTrace();
                }
            else {
                biasedActiveView.add(new BiasedInetAddress(peer, info.cost));
                activeView.add(peer);
            }
        }
    }

    private void addNewPeerToPassiveView(InetSocketAddress peer) {
        if(passiveView.size() == passiveViewMaxSize)
            removeRandomFromPassiveView();

        beginConnection(peer, -1, false, null);
    }

    private void removeRandomFromPassiveView() {
        InetSocketAddress chosen = random.fromSet(passiveView);
        passiveView.remove(chosen);

        try {
            tcp.disconnect(chosen);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void removeRandomFromActiveView() {
        InetSocketAddress chosen = random.fromSet(activeView);

        movePeerToPassiveView(chosen);
    }

    boolean isBiasedActiveViewEmpty() {
        return biasedActiveView.size() == 0;
    }

    boolean isntActiveViewFull() {
        return activeView.size() < activeViewMaxSize - futureConnections.size();
    }

    BiasedInetAddress worstBiasedPeer() {
        return biasedActiveView.last();
    }

    BiasedInetAddress beforeWorstBiasedPeer(BiasedInetAddress peer) {
        return biasedActiveView.headSet(peer).last();
    }

    void getCost(InetSocketAddress peer, XBotSupportEdge xBotSupportEdge)
            throws InterruptedException, IOException {

        oracle.getCost(peer);
        costsWaiting.put(peer, xBotSupportEdge);
    }

    boolean cantOptimize(InetSocketAddress peer1, InetSocketAddress peer2) {
        for(NeighbourhoodListener listener : neighbourhoodListeners)
            if(!listener.canOptimize(peer1, peer2))
                return true;

        return false;
    }

    void finishCycle(InetSocketAddress cycle) {
        if(cycle.equals(address))
            init = null;

        cands.remove(cycle);
        discos.remove(cycle);
        olds.remove(cycle);
    }

    InetSocketAddress chooseCand() {
        if(passiveView.size() == 0 || biasedActiveView.size() < 1)
            return null;

        return random.fromSet(passiveView);
    }

    void fireConnectionTimeout(Object peer) {
        if(peer instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) peer;

            // TODO
        }
    }
}
