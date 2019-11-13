package plumtree;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import plumtree.messages.BroadcastMessage;
import plumtree.messages.GraftMessage;
import plumtree.messages.IHaveMessage;
import plumtree.messages.PruneMessage;
import plumtree.notifications.BroadcastDeliver;
import plumtree.notifications.PeerDown;
import plumtree.notifications.PeerUp;
import plumtree.requests.BroadcastRequest;
import plumtree.timers.MissingMessageTimer;

import java.util.*;

public class Plumtree extends GenericProtocol {

    public static final short PROTOCOL_CODE = 100;
    public static final String PROTOCOL_NAME = "Plumtree";

    private static final Logger logger = LogManager.getLogger(Plumtree.class);

    private List<Host> eagerPushPeers;
    private List<Host> lazyPushPeers;

    // TODO for lazy push policies
    // private Queue<Message> lazyQueue;

    private Map<UUID, BroadcastMessage> deliveredMessages; // TODO store only temporarily
    private Map<UUID, List<IHaveAnnouncement>> missingMessages; // mIds and respective senders/rounds

    private Map<UUID, UUID> missingMessageTimers; // because I don't think different timers of the same class can exist

    private int threshold;
    private long firstTimer;
    private long secondTimer;

    public Plumtree(INetwork net)
            throws HandlerRegistrationException, NotificationDoesNotExistException, ProtocolDoesNotExist {

        super(PROTOCOL_NAME, PROTOCOL_CODE, net);

        registerNotification(BroadcastDeliver.NOTIFICATION_CODE, BroadcastDeliver.NOTIFICATION_NAME);

        registerNotificationHandler(PROTOCOL_CODE, PeerDown.NOTIFICATION_CODE, this::handlePeerDown);
        registerNotificationHandler(PROTOCOL_CODE, PeerUp.NOTIFICATION_CODE, this::handlePeerUp);

        registerRequestHandler(BroadcastRequest.REQUEST_CODE, this::broadcast);

        registerMessageHandler(BroadcastMessage.MSG_CODE, this::handleBroadcastMessage, BroadcastMessage.serializer);
        registerMessageHandler(GraftMessage.MSG_CODE, this::handleGraftMessage, BroadcastMessage.serializer);
        registerMessageHandler(IHaveMessage.MSG_CODE, this::handleIHaveMessage, IHaveMessage.serializer);
        registerMessageHandler(PruneMessage.MSG_CODE, this::handlePruneMessage, PruneMessage.serializer);

        registerTimerHandler(MissingMessageTimer.TIMER_CODE, this::handleMissingMessageTimer);
    }

    private void deliverMessage(BroadcastMessage message) {
        deliveredMessages.put(message.mId(), message);
        BroadcastDeliver deliver = new BroadcastDeliver(message.payload());
        triggerNotification(deliver);
    }

    private void eagerAndLazyPushMessage(BroadcastMessage message) {
        message.setRound(message.round() + 1);
        for(Host eagerPeer : eagerPushPeers)
            sendMessage(message, eagerPeer);

        IHaveMessage iHaveMessage = new IHaveMessage();
        iHaveMessage.setIHaveId(message.mId());
        iHaveMessage.setRound(message.round());

        for(Host lazyPeer : lazyPushPeers)
            sendMessage(iHaveMessage, lazyPeer);
    }

    // This increments the round by one, be careful??
    private void broadcast(ProtocolRequest r) {
        if(!(r instanceof BroadcastRequest))
            return;

        BroadcastRequest req = (BroadcastRequest) r;
        BroadcastMessage message = new BroadcastMessage();
        message.setPayload(req.payload());

        deliverMessage(message);

        eagerAndLazyPushMessage(message);
    }

    private void handleBroadcastMessage(ProtocolMessage m) {
        if(!(m instanceof BroadcastMessage))
            return;
        BroadcastMessage message = (BroadcastMessage) m;

        if(!deliveredMessages.containsKey(message.mId())) {
            deliverMessage(message);

            cancelMissingTimer(message.mId());

            eagerAndLazyPushMessage(message);

            eagerPushPeers.add(m.getFrom());
            lazyPushPeers.remove(m.getFrom());

            optimize(message);
        } else {
            eagerPushPeers.remove(m.getFrom());
            lazyPushPeers.add(m.getFrom());

            ProtocolMessage pruneMessage = new PruneMessage();
            sendMessage(pruneMessage, m.getFrom());
        }
    }

    private void handleGraftMessage(ProtocolMessage m) {
        if(!(m instanceof GraftMessage))
            return;

        GraftMessage message = (GraftMessage) m;

        eagerPushPeers.add(message.getFrom());
        lazyPushPeers.remove(message.getFrom());
        if(deliveredMessages.containsKey(message.graftId())) {
            byte[] payload = deliveredMessages.get(message.graftId()).payload();
            BroadcastMessage broadcastMessage = new BroadcastMessage();
            broadcastMessage.setPayload(payload).setRound(message.round() + 1);
            sendMessage(broadcastMessage, message.getFrom());
        }
    }

    private void handleIHaveMessage(ProtocolMessage m) {
        if(!(m instanceof IHaveMessage))
            return;

        IHaveMessage message = (IHaveMessage) m;

        if(!missingMessages.containsKey(message.iHaveId())) {
            if(!haveMissingMessageTimer(message.iHaveId()))
                startMissingMessageTimer(message.iHaveId(), firstTimer);
            List<IHaveAnnouncement> missingList = missingMessages.put(message.iHaveId(), new LinkedList<>());

            if(missingList == null) {
                // TODO

                System.out.println("Something went wrong adding a list to the missing map");
                System.exit(1);
            }

            missingList.add(new IHaveAnnouncement(message.getFrom(), message.round()));
        }
    }

    private void handlePruneMessage(ProtocolMessage m) {
        if(!(m instanceof PruneMessage))
            return;

        eagerPushPeers.remove(m.getFrom());
        lazyPushPeers.add(m.getFrom());
    }

    public void handlePeerDown(ProtocolNotification notification) {
        if(!(notification instanceof PeerDown))
            return;

        Host peer = ((PeerDown) notification).peer();

        eagerPushPeers.remove(peer);
        lazyPushPeers.remove(peer);

        // TODO optimize and not create new Thread???
        new Thread(() -> {
            for (Map.Entry<UUID, List<IHaveAnnouncement>> missingEntry : missingMessages.entrySet())
                for (IHaveAnnouncement announcement : missingEntry.getValue())
                    if (announcement.sender().equals(peer)) {
                        missingEntry.getValue().remove(announcement);
                        if (missingEntry.getValue().size() == 0)
                            missingMessages.remove(missingEntry.getKey());
                    }
        }).start();
    }

    public void handlePeerUp(ProtocolNotification notification) {
        if(!(notification instanceof PeerUp))
            return;

        eagerPushPeers.add(((PeerUp) notification).peer());
    }

    private void optimize(BroadcastMessage m) {
        List<IHaveAnnouncement> announcements = missingMessages.get(m.mId());

        if(announcements != null)
            for(IHaveAnnouncement announcement : announcements)
                if(announcement.round() < m.round() && m.round() - announcement.round() >= threshold) {
                    ProtocolMessage graftMessage = new GraftMessage().setGraftId(m.mId());
                    sendMessage(graftMessage, m.getFrom());

                    ProtocolMessage pruneMessage = new PruneMessage();
                    sendMessage(pruneMessage, announcement.sender());
                    break;
                }
    }

    // TODO Gotta have different timers for the same IHave from multiple peers?
    private void cancelMissingTimer(UUID mId) {
        ProtocolTimer timer = cancelTimer(mId);
        missingMessages.remove(timer.getUuid());
    }

    private void startMissingMessageTimer(UUID mId, long time) {
        MissingMessageTimer timer = new MissingMessageTimer();
        setupTimer(timer, time);
        missingMessageTimers.put(timer.getUuid(), mId);
    }

    private boolean haveMissingMessageTimer(UUID mId) {
        return missingMessageTimers.containsValue(mId);
    }

    private void handleMissingMessageTimer(ProtocolTimer timer) {
        if(!(timer instanceof MissingMessageTimer))
            return;

        UUID mId = missingMessageTimers.remove(timer.getUuid());
        if(mId == null)
            return;

        setupTimer(new MissingMessageTimer(), secondTimer); // TODO does this work? Using same object

        IHaveAnnouncement announcement = missingMessages.get(mId).remove(0);
        eagerPushPeers.add(announcement.sender());
        lazyPushPeers.remove(announcement.sender());

        GraftMessage message = new GraftMessage().setGraftId(mId).setRound(announcement.round() + 1);
        sendMessage(message, announcement.sender());
    }

    @Override
    public void init(Properties properties) {
        this.threshold = Integer.parseInt(properties.getProperty("plumtree_threshold", "100"));
        this.threshold = Integer.parseInt(properties.getProperty("plumtree_first_timer", "8000"));
        this.threshold = Integer.parseInt(properties.getProperty("plumtree_second_timer", "900"));

        this.missingMessages = new HashMap<>();
    }
}
