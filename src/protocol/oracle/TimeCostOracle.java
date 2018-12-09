package protocol.oracle;

import common.RandomChooser;
import interfaces.OracleUser;
import message.xbot.PingBackMessage;
import message.xbot.PingMessage;
import network.UDPInterface;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TimeCostOracle implements Oracle {

    private static final String ORACLE_THREAD = "Oracle Thread ";

    private UDPInterface udp;
    private InetSocketAddress address;
    private Set<OracleUser> users;

    private Map<InetSocketAddress, Long> pings;
    private BlockingQueue<CostNotification> replies;

    private RandomChooser<InetSocketAddress> random;

    private int costsSize;

    // To keep track of oracle's costs so we don't waste time recalculating
    // although we may want to sometimes just to be sure (they have a timeout)
    private Map<InetSocketAddress, CostWithTTL> costs;
    private long costTTL;

    public TimeCostOracle(InetSocketAddress address, UDPInterface udp, int repliesSize,
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
        replies = new ArrayBlockingQueue<>(repliesSize);
    }

    @Override
    public void init() {
        new Thread(this::notifyUsers, ORACLE_THREAD + address).start();
    }

    @Override
    public void getCost(InetSocketAddress dest)
            throws IOException, InterruptedException {

        CostWithTTL costTTL = costs.get(dest);

        if(costTTL != null) {
            if (costTTL.isExpired())
                costs.remove(dest);
            else {
                for(OracleUser user : users)
                    user.notifyCost(new CostNotification(dest, costTTL.cost));
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
    public void setUser(OracleUser user) {
        users.add(user);
    }

    @Override
    public void setUsers(Set<OracleUser> users) {
        this.users.addAll(users);
    }

    @Override
    public void setCostTTL(long ttl) {
        costTTL = ttl;
    }

    @Override
    public void notifyMessage(ByteBuffer msg) {
        msg.getShort();
        PingBackMessage reply = PingBackMessage.parse(msg);
        InetSocketAddress sender = reply.sender();

        Long sendTime = pings.get(sender);
        if(sendTime == null)
            return;

        long dif = System.currentTimeMillis() - sendTime;

        if(costs.size() < costsSize)
            costs.put(sender, new CostWithTTL(dif));
        else {
            Set<InetSocketAddress> set = costs.keySet();
            costs.remove(random.fromSet(set));
            costs.put(sender, new CostWithTTL(dif));
        }

        try {
            replies.put(new CostNotification(sender, dif));
        } catch(InterruptedException e) {
            // TODO
            // Annoying to user the user, but will only delay, probably
            e.printStackTrace();
        }
    }

    private void notifyUsers() {
        while(true) {
            try {
                CostNotification notification = replies.take();
                InetSocketAddress sender = notification.sender;

                Long cost = pings.get(sender);

                for(OracleUser user : users) {
                    try {
                        user.notifyCost(new CostNotification(sender, cost));
                      } catch(NullPointerException e) {
                        // For certain network sizes, collisions of forward joins might occur
                        // Just ignore it, everything is ok... probably
                    }
                }

                pings.remove(sender);
            } catch(InterruptedException e) {
                // TODO
                // Have to restart oracle
                e.printStackTrace();
            }
        }
    }

    public static class CostNotification {
        public InetSocketAddress sender;
        public long cost;

        private CostNotification(InetSocketAddress sender, long cost) {
            this.sender = sender;
            this.cost = cost;
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
