package plumtree;

import common.BroadcastListener;
import messages.plumtree.BroadcastMessage;
import messages.plumtree.GraftMessage;
import messages.plumtree.IHaveMessage;
import messages.Message;
import messages.plumtree.PruneMessage;
import network.Network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class Plumtree implements TreeBroadcast {

    private Network network;

    private Collection<InetSocketAddress> eagerPushPeers;
    private Collection<InetSocketAddress> lazyPushPeers;

    private Queue<Message> lazyQueue;

    private Map<UUID, BroadcastMessage> receivedMessages;
    private Map<UUID, List<IHaveAnnouncement>> missingMessages; // mIds and respective senders/rounds

    private InetSocketAddress id;

    private BroadcastListener broadcastListener;

    public Plumtree() {

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
        } else {
            eagerPushPeers.remove(m.sender());
            lazyPushPeers.add(m.sender());
            network.send(new PruneMessage(id).serialize(), m.sender());
        }
    }

    private void handleIHaveMessage(IHaveMessage m) {
        if(!missingMessages.containsKey(m.iHaveId())) {
            if (!haveMissingTimer(m.iHaveId()))
                startMissingTimer(m.iHaveId());
            List<IHaveAnnouncement> missings = missingMessages.put(m.id(), new LinkedList<>());

            if(missings == null) {
                // TODO

                System.out.println("Something went wrong adding a list to the missing map");
                System.exit(1);
            }

            missings.add(new IHaveAnnouncement(m.sender(), m.round()));
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

    private void cancelMissingTimer(UUID mId) {
        // TODO
    }

    private void startMissingTimer(UUID mId) {
        // TODO
    }

    private boolean haveMissingTimer(UUID mId) {
        // TODO

        return true;
    }
}
