package protocol;

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

public class XBotNode implements OptimizerNode {

    // Names for the timers (threads)
    private static final String OPTI = "O";
    private static final String WAIT = "W";

    final private InetSocketAddress id;

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
    private Map<InetSocketAddress, Integer> costsWaiting;
    private InetSocketAddress init;
    private InetSocketAddress cand;
    private InetSocketAddress old;
    // private InetSocketAddress disc;
    private long itoo;
    private long itoc;
    private long ctod;
    private long dtoo;

    // Each constant integer is a code for what has to be done
    // private static final int ITOO = 1;
    private static final int ITOC = 2;
    private static final int CTOD = 3;
    private static final int DTOO = 4;
    // private static final int NEW = 5;
    private static final int JOIN = 6;

    private int attl;
    private int pttl;

    private PersistantNetwork tcp;

    private Set<NeighbourhoodListener> neighbourhoodListeners;

    // For testing, every node waits a random (relatively low) amount of time
    // when answering pings
    private long pingSleepTime;

    XBotNode(InetSocketAddress id, int activeViewMaxSize,
             int unbiasedViewMaxSize, int passiveViewMaxSize,
             int optimizationPeriod, long waitTimeOut, int attl, int pttl)
            throws IllegalArgumentException {

        if(activeViewMaxSize <= 0 || id == null)
            throw new IllegalArgumentException();

        this.id = id;
        this.unbiasedViewMaxSize = unbiasedViewMaxSize;
        this.activeViewMaxSize = activeViewMaxSize;
        this.passiveViewMaxSize = passiveViewMaxSize;

        itoo = 0;
        itoc = Long.MAX_VALUE;
        ctod = 0;
        dtoo = 0;

        init = null;
        cand = null;
        old = null;
        // disc = null;

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

    private void handleForwardJoin(ByteBuffer bytes) {
        ForwardJoinMessage msg = ForwardJoinMessage.parse(bytes);

        try {
            if(msg.ttl() == 0 || activeView.size() <= 1) {
                if(addPeerToActiveView(msg.joiner(), -1)) {
                    Message accept = new AcceptJoinMessage(id);
                    sendAcceptJoin(accept, msg.sender());
                }
            } else {
                if(msg.ttl() == pttl)
                    addPeerToPassiveView(msg.joiner());

                InetSocketAddress peer = random.fromSet(activeView);

                while(peer.equals(msg.sender()) || peer.equals(msg.joiner()))
                    peer = random.fromSet(activeView);

                tcp.send(msg.next(id).bytes(), peer);
            }
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleOptimization(ByteBuffer bytes) {
        OptimizationMessage msg = OptimizationMessage.parse(bytes);

        if(/*optimizing || */biasedActiveView.size() == 0) {
            Message reply = new OptimizationReplyMessage(id, false, false);

            try {
                tcp.send(reply.bytes(), msg.sender());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }

            return;
        }

        // optimizing = true;

        init = msg.sender();
        old = msg.old();
        itoo = msg.itoo();
        itoc = msg.itoc();

        Message replace = new ReplaceMessage(id, init, old, itoo, itoc);

        try {
            tcp.send(replace.bytes(), biasedActiveView.last().address);
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleOptimizationReply(ByteBuffer bytes) {
        OptimizationReplyMessage msg = OptimizationReplyMessage.parse(bytes);

        try {
            if(msg.accept()) {
                removeFromBiased(old);
                addPeerToActiveView(cand, itoc);

                Message reply = new DisconnectMessage(id, msg.removed());

                tcp.send(reply.bytes(), old);
            }
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }

        // optimizing = false;

        itoo = 0;
        itoc = Long.MAX_VALUE;
        ctod = 0;
        dtoo = 0;
    }

    private void handleReplace(ByteBuffer bytes) {
        ReplaceMessage msg = ReplaceMessage.parse(bytes);

        // optimizing = true;

        init = msg.init();
        old = msg.old();
        itoo = msg.itoo();
        itoc = msg.itoc();

        try {
            oracle.getCost(msg.old());
            costsWaiting.put(msg.old(), DTOO);

            oracle.getCost(msg.sender());
            costsWaiting.put(msg.sender(), CTOD);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleReplaceReply(ByteBuffer bytes) {
        ReplaceReplyMessage msg = ReplaceReplyMessage.parse(bytes);

        boolean removed = false;

        if(msg.accept()) {
            removed = removeFromBiased(msg.sender());

            addPeerToBiasedActiveView(init, itoc);
        }

        try {
            Message optimizationReply = new OptimizationReplyMessage(id, msg.accept(), removed);

            tcp.send(optimizationReply.bytes(), init);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }

        // optimizing = false;

        itoo = 0;
        itoc = Long.MAX_VALUE;
        ctod = 0;
        dtoo = 0;
    }

    private void handlePing(ByteBuffer bytes) {
        PingMessage msg = PingMessage.parse(bytes);

        Message pingBackMsg = new PingBackMessage(id);

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
        return id;
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

        timerManager.addTimer(OPTI, this::optimizeStep1, optimizationPeriod);

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
            Message accept = new AcceptJoinMessage(id);
            sendAcceptJoin(accept, msg.sender());
        }

        try {
            activeView.forEach((peer) -> {
                Message forwardMsg = new ForwardJoinMessage(id, msg.sender(), attl);

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

        if(tcpNoti.accept())
            return;

        Message joinMsg = new JoinMessage(id);

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

        Integer code = costsWaiting.remove(sender);

        if(code == null)
            return;

        if(code == JOIN)
            try {
                addPeerToActiveView(sender, cost);
                Message acceptMsg = new AcceptJoinMessage(id);
                tcp.send(acceptMsg.bytes(), sender);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }

        else if(code == ITOC)
            optimizeStep2(sender, cost);

        else if(code == DTOO)
            optimizeStep3_1(sender, cost);

        else if(code == CTOD)
            optimizeStep3_2(sender, cost);

        else
            System.out.println("??? unrecognized code");
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

    private void optimizeStep1() {
        if(/*optimizing || */passiveView.size() == 0 || biasedActiveView.size() < 1)
            return;

        // optimizing = true;
        InetSocketAddress cand = random.fromSet(passiveView);
        try {
            oracle.getCost(cand);
            costsWaiting.put(cand, ITOC);
        } catch(IOException | InterruptedException e) {
            // TODO
            // optimizing = false;
            e.printStackTrace();
        }
    }

    private void optimizeStep2(InetSocketAddress cand, long itoc) {
        if(this.cand != null)
            return;

        if(biasedActiveView.size() < 1) {
            System.out.println("??? optimizing with empty biased view");
            // optimizing = false;

            return;
        }

        this.itoc = itoc;
        this.cand = cand;

        BiasedInetAddress old = biasedActiveView.last();

        this.itoo = old.cost;
        this.old = old.address;

        Message msg = new OptimizationMessage(id, old.address, itoo, itoc);

        try {
            tcp.send(msg.bytes(), cand);
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void optimizeStep3_1(InetSocketAddress old, long dtoo) {
        this.dtoo = dtoo;
        this.old = old;

        if(ctod != 0)
            optimizeStep3_3();
    }

    private void optimizeStep3_2(InetSocketAddress cand, long ctod) {
        this.ctod = ctod;
        this.cand = cand;

        if(dtoo != 0)
            optimizeStep3_3();

    }

    private void optimizeStep3_3() {
        try {
            if(itsWorthOptimizing(this::basicComparer, itoo, itoc, ctod, dtoo)) {

                Message replaceReply = new ReplaceReplyMessage(id, true);
                Message switchMessage = new SwitchMessage(id, init, dtoo);

                tcp.send(replaceReply.bytes(), cand);
                tcp.send(switchMessage.bytes(), old);

                removeFromBiased(cand);

                addPeerToBiasedActiveView(old, dtoo);

                // optimizing = false;

                itoo = 0;
                itoc = Long.MAX_VALUE;
                dtoo = 0;
                ctod = 0;
            } else {
                Message replaceReply = new ReplaceReplyMessage(id, false);

                tcp.send(replaceReply.bytes(), cand);

                // optimizing = false;

                itoo = 0;
                itoc = Long.MAX_VALUE;
                dtoo = 0;
                ctod = 0;
            }
        } catch(IllegalArgumentException | IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private boolean addPeerToActiveView(InetSocketAddress peer, long cost) {
        try {
            if(peer.equals(id))
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }

        if((activeView.size() == activeViewMaxSize - 1 && waiting) || activeView.size() == activeViewMaxSize)
            removeRandomFromActive();

        if(unbiasedActiveView.size() < unbiasedViewMaxSize) {
            unbiasedActiveView.add(peer);
            activeView.add(peer);

            passiveView.remove(peer);

            for(NeighbourhoodListener listener : neighbourhoodListeners)
                listener.neighbourUp(peer);

            return true;
        } else if(cost == -1)
            try {
                oracle.getCost(peer);

                costsWaiting.put(peer, JOIN);

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

            return true;
        }
    }

    private void addPeerToBiasedActiveView(InetSocketAddress peer, long cost) {
        try {
            if (peer.equals(id))
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }

        activeView.add(peer);
        biasedActiveView.add(new BiasedInetAddress(peer, cost));

        for(NeighbourhoodListener listener : neighbourhoodListeners)
            listener.neighbourUp(peer);
    }

    private void removeRandomFromActive() {
        int choose = random.integer(2);

        InetSocketAddress disconnect = choose == 0 ?
                removeRandomFromUnbiased()
                : removeWorstActivePeer();

        addPeerToPassiveView(disconnect);

        Message msg = new DisconnectMessage(id, false);

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
        random.removeFromSet(passiveView);
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

    private boolean removeFromBiased(InetSocketAddress peer) {
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

    private static class BiasedInetAddress implements Comparable<BiasedInetAddress> {

        private InetSocketAddress address;
        private long cost;

        private BiasedInetAddress(InetSocketAddress address, long cost) {
            this.address = address;
            this.cost = cost;
        }

        @Override
        public int compareTo(BiasedInetAddress o) {
            if(address.equals(o.address))
                return 0;
            else if(cost - o.cost < 0)
                return -1;
            else
                return 1;
        }
    }

    private boolean itsWorthOptimizing(CostComparer comparer, long itoo,
                                       long itoc, long ctod, long dtoo) {
        return comparer.compare(itoc, itoo, ctod, dtoo);
    }

    private boolean basicComparer(long itoo, long itoc, long ctod, long dtoo) {
        return itoc + dtoo < itoo + ctod;
    }
}
