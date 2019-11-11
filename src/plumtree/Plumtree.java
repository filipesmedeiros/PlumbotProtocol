package plumtree;

import common.BroadcastListener;
import messages.plumtree.BroadcastMessage;
import messages.plumtree.GraftMessage;
import messages.plumtree.IHaveMessage;
import messages.Message;
import messages.plumtree.PruneMessage;
import network.Network;
import xbot.PeerSampling;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class Plumtree implements TreeBroadcast {

    private Network network;

    private Collection<InetSocketAddress> eagerPushPeers;
    private Collection<InetSocketAddress> lazyPushPeers;

    // TODO for lazy push policies
    // private Queue<Message> lazyQueue;

    private Map<UUID, BroadcastMessage> receivedMessages;
    private Map<UUID, List<IHaveAnnouncement>> missingMessages; // mIds and respective senders/rounds

    private InetSocketAddress id;

    private BroadcastListener broadcastListener; // TODO

    private PeerSampling peerSampling; // TODO why do we need this after constructor?

    private int threshold;
    private long firstTimer;
    private long secondTimer;

    public Plumtree(InetSocketAddress id, int threshold, long firstTimer, long secondTimer, PeerSampling peerSampling) {
        eagerPushPeers = new HashSet<>();
        lazyPushPeers = new HashSet<>();

        receivedMessages = new HashMap<>();
        missingMessages = new HashMap<>();

        this.id = id;

        this.peerSampling = peerSampling;
        eagerPushPeers.addAll(peerSampling.getPeers());

        this.threshold = threshold;
        this.firstTimer = firstTimer;
        this.secondTimer = secondTimer;
    }

    public void setBroadcastListener(BroadcastListener broadcastListener) {
        this.broadcastListener = broadcastListener;
    }

    @Override
    public void broadcast(ByteBuffer data) {
        BroadcastMessage m = new BroadcastMessage(id, data, 0);
        eagerPush(m);
        lazyPush(m);
        broadcastListener.deliver(m.data());
        receivedMessages.put(m.id(), m);
    }

    @Override
    public void peerDown(InetSocketAddress peer) {
        eagerPushPeers.remove(peer);
        lazyPushPeers.remove(peer);

        for(Map.Entry<UUID, List<IHaveAnnouncement>> missingEntry : missingMessages.entrySet())
            for(IHaveAnnouncement announcement : missingEntry.getValue())
                if(announcement.sender().equals(peer)) {
                    missingEntry.getValue().remove(announcement);
                    if(missingEntry.getValue().size() == 0)
                        missingMessages.remove(missingEntry.getKey());
                }
    }

    @Override
    public void peerUp(InetSocketAddress peer) {
        eagerPushPeers.add(peer);
    }

    @Override
    public void deliverMessage(Message m) {
        switch(m.type()) {
            case Broadcast:
                handleBroadcastMessage((BroadcastMessage) m);
                break;
            case IHave:
                handleIHaveMessage((IHaveMessage) m);
                break;
            case Graft:
                handleGraftMessage((GraftMessage) m);
                break;
            case Prune:
                handlePrune((PruneMessage) m);
                break;
        }
    }

    private void handleBroadcastMessage(BroadcastMessage m) {
        if(!receivedMessages.containsKey(m.id())) {
            broadcastListener.deliver(m.data());
            receivedMessages.put(m.id(), m);

            cancelMissingTimer(m.id());

            BroadcastMessage newBroadcastMessage = new BroadcastMessage(id, m);
            eagerPush(newBroadcastMessage);
            lazyPush(newBroadcastMessage);

            eagerPushPeers.add(m.sender());
            lazyPushPeers.remove(m.sender());

            optimize(m);
        } else {
            eagerPushPeers.remove(m.sender());
            lazyPushPeers.add(m.sender());
            network.send(new PruneMessage(id).serialize(), m.sender());
        }
    }

    private void handleIHaveMessage(IHaveMessage m) {
        if(!missingMessages.containsKey(m.iHaveId())) {
            if(!haveMissingTimer(m.iHaveId()))
                startMissingTimer(m.iHaveId(), firstTimer);
            List<IHaveAnnouncement> missingList = missingMessages.put(m.id(), new LinkedList<>());

            if(missingList == null) {
                // TODO

                System.out.println("Something went wrong adding a list to the missing map");
                System.exit(1);
            }

            missingList.add(new IHaveAnnouncement(m.sender(), m.round()));
        }
    }

    // TODO in the specs, there is a round here, why?
    private void handleGraftMessage(GraftMessage m) {
        eagerPushPeers.add(m.sender());
        lazyPushPeers.remove(m.sender());
        if(receivedMessages.containsKey(m.graftId())) {
            BroadcastMessage broadcastMessage = new BroadcastMessage(id, receivedMessages.get(m.graftId()));
            network.send(broadcastMessage.serialize(), m.sender());
        }
    }

    private void handlePrune(PruneMessage m) {
        eagerPushPeers.remove(m.sender());
        lazyPushPeers.add(m.sender());
    }

    private void eagerPush(BroadcastMessage m) {
        for(InetSocketAddress peer : eagerPushPeers)
            if(!peer.equals(m.sender()))
                network.send(m.serialize(), peer);
    }

    private void lazyPush(BroadcastMessage m) {
        for(InetSocketAddress peer : lazyPushPeers)
            if(!peer.equals(m.sender()))
                network.send(new IHaveMessage(id, m.id(), m.round() + 1).serialize(), peer);
    }

    private void optimize(BroadcastMessage m) {
        List<IHaveAnnouncement> announcements = missingMessages.get(m.id());

        if(announcements != null)
            for(IHaveAnnouncement announcement : announcements)
                if(announcement.round() < m.round() && m.round() - announcement.round() >= threshold) {
                    network.send(new GraftMessage(id, new UUID(0, 0)).serialize(), m.sender());
                    network.send(new PruneMessage(id).serialize(), announcement.sender());

                    announcements.remove(announcement);
                    break;
                }
    }

    private void cancelMissingTimer(UUID mId) {
        // TODO
    }

    private void startMissingTimer(UUID mId, long time) {
        // TODO
    }

    private boolean haveMissingTimer(UUID mId) {
        // TODO

        return true;
    }

    public void fireMissingTimer(UUID mId) {
        startMissingTimer(mId, secondTimer);
        IHaveAnnouncement announcement = missingMessages.get(mId).remove(0);
        eagerPushPeers.add(announcement.sender());
        lazyPushPeers.remove(announcement.sender());
        network.send(new GraftMessage(id, mId).serialize(), announcement.sender());
    }
}
