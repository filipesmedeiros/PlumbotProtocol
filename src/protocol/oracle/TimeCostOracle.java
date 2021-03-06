package protocol.oracle;

import common.RandomChooser;
import interfaces.Notifiable;
import message.xbot.PingBackMessage;
import message.xbot.PingMessage;
import network.NetworkInterface;
import notifications.CostNotification;
import notifications.MessageNotification;
import notifications.Notification;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TimeCostOracle implements Oracle {

    private static final String ORACLE_THREAD = "Oracle Thread ";

    private NetworkInterface udp;
    private InetSocketAddress address;
    private Set<Notifiable> users;

    private Map<InetSocketAddress, Long> pings;
    private BlockingQueue<Notification> notifications;

    private RandomChooser<InetSocketAddress> random;

    private int costsSize;

    // To keep track of oracle's costs so we don't waste time recalculating
    // although we may want to sometimes just to be sure (they have a timeout)
    private Map<InetSocketAddress, CostWithTTL> costs;
    private long costTTL;

    public TimeCostOracle(InetSocketAddress address, NetworkInterface udp, int repliesSize,
                          int costsSize, long costTTL) throws IllegalArgumentException {

        if(address == null || udp == null || repliesSize <= 0 || costsSize <= 0)
            throw new IllegalArgumentException();

        this.udp = udp;
        this.address = address;
        this.costsSize = costsSize;

        this.costTTL = costTTL;

        pings = new HashMap<>();

        random = new RandomChooser<>();

        costs = new HashMap<>();

        users = new HashSet<>();
        notifications = new ArrayBlockingQueue<>(repliesSize);
    }

    @Override
    public void init() {
        processNotification();
    }

    @Override
    public void getCost(InetSocketAddress dest)
            throws IOException, InterruptedException {

        CostWithTTL costTTL = costs.get(dest);

        if(costTTL != null) {
            if (costTTL.isExpired())
                costs.remove(dest);
            else {
                for(Notifiable user : users)
                    user.notify(new CostNotification(dest, costTTL.cost));
                return;
            }
        }

        udp.send(new PingMessage(address).bytes(), dest);

        Long prevSendTime = pings.get(dest);
        if(prevSendTime == null)
            pings.put(dest, System.currentTimeMillis());
        else
            // Should be used to penalize nodes that have to be retried
            pings.put(dest, System.currentTimeMillis());
    }

    @Override
    public void setUser(Notifiable user) {
        users.add(user);
    }

    @Override
    public void setUsers(Set<Notifiable> users) {
        this.users.addAll(users);
    }

    @Override
    public void setCostTTL(long ttl) {
        costTTL = ttl;
    }

    // Should only receive PingBackMessages
    @Override
    public void notify(Notification notification) {
        if(!(notification instanceof MessageNotification)) {
            System.out.println("??? Wrong message notification");
            return;
        }

        ByteBuffer msg = ((MessageNotification) notification).message();

        msg.getShort();
        PingBackMessage reply = PingBackMessage.parse(msg);
        InetSocketAddress sender = reply.sender();

        Long sendTime = pings.get(sender);
        if(sendTime == null)
            return;

        long dif = System.currentTimeMillis() - sendTime;

        // TODO Set should be ordered and should remove oldest cost
        if(costs.size() >= costsSize) {
            Set<InetSocketAddress> set = costs.keySet();
            costs.remove(random.fromSet(set));
        }

        costs.put(sender, new CostWithTTL(dif));

        try {
            notifications.put(new CostNotification(sender, dif));
        } catch(InterruptedException e) {
            // TODO
            // Annoying to user the user, but will only delay, probably
            e.printStackTrace();
        }
    }

    @Override
    public boolean setNetwork(NetworkInterface udp)
            throws IllegalArgumentException {

        if(udp == null)
            throw new IllegalArgumentException();

        this.udp = udp;

        return true;
    }

    private void processNotification() {
        while(true) {
            try {
                Notification notification = notifications.take();

                if(notification.type() != CostNotification.TYPE) {
                    System.out.println("??? Wrong cost notification");
                    continue;
                }

                CostNotification costNoti = (CostNotification) notification;

                for(Notifiable user : users) {
                    try {

                        user.notify(costNoti);
                    } catch(NullPointerException e) {
                        // For certain network sizes, collisions of forward joins might occur
                        // Just ignore it, everything is ok... probably
                    }
                }

                pings.remove(costNoti.sender());
            } catch(InterruptedException e) {
                // TODO
                // Have to restart oracle
                e.printStackTrace();
            }
        }
    }

    private class CostWithTTL {
        private long cost;
        private long registeredAt;

        private CostWithTTL(long cost) {
            this.cost = cost;
            registeredAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return registeredAt + costTTL > System.currentTimeMillis();
        }
    }
}
