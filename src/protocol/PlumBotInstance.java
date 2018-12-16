package protocol;

import exceptions.NotReadyForInitException;
import interfaces.OptimizerNode;
import message.plumtree.*;
import message.xbot.*;
import network.PersistantNetwork;
import network.TCP;
import network.UDP;
import network.NetworkInterface;
import protocol.oracle.Oracle;
import protocol.oracle.TimeCostOracle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlumBotInstance implements PlumBot {

    private static final int ONE_MINUTE = 1000 * 60;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

    private OptimizerNode xbot;
    private TreeBroadcastNode plum;
    private NetworkInterface udp;
    private PersistantNetwork tcp;

    private InetSocketAddress id;

    public PlumBotInstance(InetSocketAddress local) {
        this.id = local;

        try {
            udp = new UDP(local);

            tcp = new TCP(local);

            xbot = new XBotNode(local, 5, 1, 10, 1000 * 30, ONE_MINUTE, 4, 2);

            plum = new PlumtreeNode(local, 3, 5, FIVE_MINUTES, ONE_MINUTE, FIVE_MINUTES);

            Oracle oracle = new TimeCostOracle(local, udp, 10, 10, FIVE_MINUTES);

            tcp.setConnector(xbot);
            tcp.addMessageListener(xbot, xbotMessageTypes());
            tcp.addMessageListener(plum, plumtreeMessageTypes());

            udp.addMessageListener(oracle, oracleMessageTypes());

            xbot.setOracle(oracle);
            xbot.setNetwork(tcp);

            plum.setNetwork(tcp);

            oracle.setUser(xbot);

            new Thread(() -> {
                try {
                    udp.initialize();
                } catch(NotReadyForInitException | IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    tcp.initialize();
                } catch(NotReadyForInitException | IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    plum.initialize();
                } catch(NotReadyForInitException e) {
                    // TODO
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    xbot.initialize();
                } catch(NotReadyForInitException e) {
                    // TODO
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void broadcast(ByteBuffer bytes) throws IllegalArgumentException {
        plum.broadcast(bytes);
    }

    @Override
    public void join(InetSocketAddress contact) {
        xbot.join(contact);
    }

    @Override
    public Set<InetSocketAddress> peerActiveView() {
        return xbot.activeView();
    }

    @Override
    public Set<InetSocketAddress> treeActiveView() {
        // TODO
        return null;
    }

    @Override
    public Set<InetSocketAddress> passiveView() {
        return xbot.passiveView();
    }

    @Override
    public InetSocketAddress id() {
        return id;
    }

    private List<Short> xbotMessageTypes() {
        List<Short> list = new ArrayList<>(10);
        list.add(JoinMessage.TYPE);
        list.add(AcceptJoinMessage.TYPE);
        list.add(ForwardJoinMessage.TYPE);
        list.add(OptimizationMessage.TYPE);
        list.add(OptimizationReplyMessage.TYPE);
        list.add(ReplaceMessage.TYPE);
        list.add(ReplaceReplyMessage.TYPE);
        list.add(DisconnectMessage.TYPE);
        list.add(SwitchMessage.TYPE);
        list.add(PingMessage.TYPE);

        return list;
    }

    private List<Short> plumtreeMessageTypes() {
        List<Short> list = new ArrayList<>(5);
        list.add(PruneMessage.TYPE);
        list.add(BodyMessage.TYPE);
        list.add(GraftMessage.TYPE);
        list.add(IHaveMessage.TYPE);
        list.add(RequestMessage.TYPE);

        return list;
    }

    private List<Short> oracleMessageTypes() {
        List<Short> list = new ArrayList<>(1);
        list.add(PingBackMessage.TYPE);

        return list;
    }
}
