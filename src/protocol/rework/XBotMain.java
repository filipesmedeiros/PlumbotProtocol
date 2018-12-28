package protocol.rework;

import common.MappedTimerManager;
import common.RandomChooser;
import common.TimerManager;
import exceptions.NotReadyForInitException;
import interfaces.CostComparer;
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class XBotMain implements OptimizerNode {

    // Names for the timers (threads)
    private static final String OPTI = "O";
    private static final String WAIT = "W";

    final private InetSocketAddress address;

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

    // used in previous versions for allowing a node to only participate in an optimizing cycle at a time
    // private boolean optimizing;

    private boolean waiting;
    private long waitTimeout;

    // All these are for remembering stuff during asynch optimization
    private Map<InetSocketAddress, XBotSupportEdge> costsWaiting;

    // Each constant integer is a code for what has to be done
    // private static final int ITOO = 1;
    static final int I_TO_C = 2;
    static final int C_TO_D = 3;
    static final int D_TO_O = 4;
    static final int JOIN = 5;
    // static final int NEW = 6;

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

    XBotMain(InetSocketAddress address, int activeViewMaxSize,
             int unbiasedViewMaxSize, int passiveViewMaxSize,
             int optimizationPeriod, long waitTimeOut, int attl, int pttl)
            throws IllegalArgumentException {

        if(activeViewMaxSize <= 0 || address == null)
            throw new IllegalArgumentException();

        this.address = address;
        this.unbiasedViewMaxSize = unbiasedViewMaxSize;
        this.activeViewMaxSize = activeViewMaxSize;
        this.passiveViewMaxSize = passiveViewMaxSize;

        init = null;
        cands = new HashMap<>();
        discos = new HashMap<>();
        olds = new HashMap<>();

        this.attl = attl;
        this.pttl = pttl;

        // optimizing = false;
        this.optimizationPeriod = optimizationPeriod;

        waiting = false;
        this.waitTimeout = waitTimeOut;

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

        discos.put(msg.cycle(), disco);
    }

    private void handleReplaceReply(ByteBuffer bytes) {
        ReplaceReplyMessage msg = ReplaceReplyMessage.parse(bytes);

        cands.get(msg.cycle()).handleReplaceReply(msg);
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

    private void handleDisconnect(ByteBuffer bytes) {
        DisconnectMessage msg = DisconnectMessage.parse(bytes);

        InetSocketAddress peer = msg.sender();

        if(!removeFromActive(peer))
            return;

        if(msg.hasToWait()) {
            waiting = true;
            timerManager.addAction(WAIT, () -> this.waiting = false, waitTimeout);
        }

        try {
            tcp.disconnect(peer);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleSwitch(ByteBuffer bytes) {
        SwitchMessage msg = SwitchMessage.parse(bytes);

        addPeerToActiveView(msg.sender(), msg.dtoo());
        removeFromActive(msg.init());
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
        if(size <= 0)
            throw new IllegalArgumentException();

        int num = 0;
        while(unbiasedActiveView.size() > size) {
            removeRandomFromUnbiased();
            num++;
        }

        unbiasedViewMaxSize = size;
        return num;
    }

    @Override
    public int setAViewSize(int size) throws IllegalArgumentException {
        if(size <= unbiasedViewMaxSize)
            throw new IllegalArgumentException();

        int num = 0;
        while(activeView.size() > size) {
            // TODO guarantees about unbiased peer number after this
            removeRandomFromActive();
            num++;
        }

        activeViewMaxSize = size;
        return num;
    }

    @Override
    public int setPViewSize(int size) throws IllegalArgumentException {
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

        new Thread(() -> oracle.init());

        processNotification();
    }

    @Override
    public void join(InetSocketAddress contact) {
        if(contact == null) return;

        try {
            tcp.connect(contact);
        } catch(IllegalArgumentException | IOException e) {
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

    private void sendAcceptJoin(Message accept, InetSocketAddress sender) {
        try {
            tcp.send(accept.bytes(), sender);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleJoin(ByteBuffer bytes) {
        JoinMessage msg = JoinMessage.parse(bytes);

        System.out.println("sender of join -> " + msg.sender());

        if(addPeerToActiveView(msg.sender(), -1)) {
            System.out.println("uiglgli");
            Message accept = new AcceptJoinMessage(address);
            sendAcceptJoin(accept, msg.sender());
        }

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
            if(msg.ttl() == 0 || activeView.size() <= 1) {
                if(addPeerToActiveView(msg.joiner(), -1)) {
                    Message accept = new AcceptJoinMessage(address);
                    sendAcceptJoin(accept, msg.sender());
                    tcp.connect(msg.sender());
                }
            } else {
                if(msg.ttl() == pttl)
                    addPeerToPassiveView(msg.joiner());

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
        addPeerToActiveView(msg.sender(), -1);
    }

    private void handleConnection(Notification notification) {
        if(!(notification instanceof TCPConnectionNotification)) {
            System.out.println("??? Wrong connection notification");
            return;
        }

        TCPConnectionNotification tcpNoti = (TCPConnectionNotification) notification;

        System.out.println(address + " handling connection from " + tcpNoti.peer());

        if(tcpNoti.accept())
            return;

        System.out.println(address + " and it's a new node");

        Message joinMsg = new JoinMessage(address);

        try {
            tcp.send(joinMsg.bytes(), tcpNoti.peer());
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

            if(xBotSupportEdge == null) {
                try {
                    addPeerToActiveView(sender, cost);
                    tcp.connect(sender);
                } catch(IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            } else {
                xBotSupportEdge.handleCost(sender, cost);
            }
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
        init = new XBotInit(this, tcp);

        init.optimize();
    }

    private boolean addPeerToActiveView(InetSocketAddress peer, long cost) {
        try {
            if(peer.equals(address))
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }

        if((activeView.size() == activeViewMaxSize - 1 && waiting)
                || activeView.size() == activeViewMaxSize)
            removeRandomFromActive();

        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);

            passiveView.remove(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourUp(peer);

            try {
                tcp.connect(peer);
            } catch(IOException e) {
                // TODO
                e.printStackTrace();
            }

            return true;
        } else if(cost == -1)
            try {
                oracle.getCost(peer);

                costsWaiting.put(peer, null);

                return false;
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();

                return false;
            }
        else {
            biasedActiveView.add(new BiasedInetAddress(peer, cost));
            activeView.add(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourUp(peer);

            passiveView.remove(peer);

            try {
                tcp.connect(peer);
            } catch(IOException e) {
                // TODO
                e.printStackTrace();
            }

            return true;
        }
    }

    void addPeerToBiasedActiveView(InetSocketAddress peer, long cost) {
        try {
            if (peer.equals(address))
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }

        activeView.add(peer);
        biasedActiveView.add(new BiasedInetAddress(peer, cost));

        try {
            tcp.connect(peer);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourUp(peer);
    }

    private void removeRandomFromActive() {
        int choose = random.integer(2);

        InetSocketAddress disconnect = choose == 0 ?
                removeRandomFromUnbiased()
                : removeWorstActivePeer();

        addPeerToPassiveView(disconnect);

        Message msg = new DisconnectMessage(address, false);

        try {
            tcp.send(msg.bytes(), disconnect);
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private InetSocketAddress removeRandomFromUnbiased() {
        InetSocketAddress chosen = random.fromSet(unbiasedActiveView);
        unbiasedActiveView.remove(chosen);
        activeView.remove(chosen);

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourDown(chosen);

        return chosen;
    }

    private InetSocketAddress removeWorstActivePeer() {
        BiasedInetAddress worst = biasedActiveView.last();
        biasedActiveView.remove(worst);
        activeView.remove(worst.address);

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourDown(worst.address);

        return worst.address;
    }

    private void addPeerToPassiveView(InetSocketAddress id) {
        if(passiveView.size() == passiveViewMaxSize)
            removeRandomFromPassiveView();

        passiveView.add(id);
    }

    private void removeRandomFromPassiveView() {
        InetSocketAddress peer = random.fromSet(passiveView);

        try {
            tcp.disconnect(peer);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }

        passiveView.remove(peer);
    }

    private boolean removeFromActive(InetSocketAddress peer) {
        boolean removed = activeView.remove(peer);

        if(removed) {
            removeFromBiased(peer);
            unbiasedActiveView.remove(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourDown(peer);
        }

        addPeerToPassiveView(peer);

        return removed;
    }

    boolean removeFromBiased(InetSocketAddress peer) {
        activeView.remove(peer);

        for(BiasedInetAddress aPeer : biasedActiveView) {
            if(aPeer.address.equals(peer)) {
                addPeerToPassiveView(peer);

                return biasedActiveView.remove(aPeer);
            }
        }

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourDown(peer);

        return false;
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

    boolean isActiveViewEmpty() {
        return activeView.size() == 0;
    }

    boolean isBiasedActiveViewEmpty() {
        return biasedActiveView.size() == 0;
    }

    boolean isUnbiasedActiveViewEmpty() {
        return unbiasedActiveView.size() == 0;
    }

    boolean isPassiveViewEmpty() {
        return passiveView.size() == 0;
    }

    boolean isActiveViewFull() {
        return activeView.size() == activeViewMaxSize;
    }

    boolean isBiasedActiveViewFull() {
        return biasedActiveView.size() == activeViewMaxSize - unbiasedViewMaxSize;
    }

    boolean isUnbiasedActiveViewFull() {
        return unbiasedActiveView.size() == unbiasedViewMaxSize;
    }

    boolean isPassiveViewFull() {
        return passiveView.size() == passiveViewMaxSize;
    }

    BiasedInetAddress worstBiasedPeer() {
        return biasedActiveView.last();
    }

    void getCost(InetSocketAddress peer, XBotSupportEdge xBotSupportEdge)
            throws InterruptedException, IOException {

        oracle.getCost(peer);
        costsWaiting.put(peer, xBotSupportEdge);
    }

    boolean canOptimize(InetSocketAddress peer1, InetSocketAddress peer2) {
        for(NeighbourhoodListener listener : neighbourhoodListeners)
            if(!listener.canOptimize(peer1, peer2))
                return false;

        return true;
    }

    void finishCycle(InetSocketAddress cycle) {
        cands.remove(cycle);
        discos.remove(cycle);
        olds.remove(cycle);
    }

    InetSocketAddress chooseCand() {
        if(passiveView.size() == 0 || biasedActiveView.size() < 1)
            return null;

        return random.fromSet(passiveView);
    }
}
