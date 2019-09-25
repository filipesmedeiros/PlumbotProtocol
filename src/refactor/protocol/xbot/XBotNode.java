package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import jdk.nashorn.internal.runtime.GlobalConstants;
import refactor.GlobalSettings;
import refactor.message.Message;
import refactor.message.MessageDecoder;
import refactor.network.TCP;
import refactor.protocol.AbstractNode;
import refactor.protocol.notifications.CostNotification;
import refactor.protocol.notifications.MessageNotification;
import refactor.protocol.notifications.Notification;
import refactor.protocol.notifications.TimerNotification;
import refactor.protocol.oracle.RTTOracle;
import refactor.utils.*;
import refactor.protocol.Node;

/**
 * To know more about the XBot protocol, read http://asc.di.fct.unl.pt/~jleitao/pdf/srds09-leitao.pdf
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 27.06.2019
 */
public class XBotNode extends AbstractNode {

    public static final String OLD_ADDRESS_LABEL = "old";

    public static final String JOINER_LABEL = "jnr";

    public static final String ACTIVE_RWL_LABEL = "arwl";

    public static final String JOIN_COST_LABEL = "jnc";

    public static final String OPTIMIZATION_REPLY_LABEL = "optr";

    public static final String REPLACE_REPLY_LABEL = "rplr";

    public static final String NO_DISCONNECT_LABEL = "ndsc";

    public static final String INITIATOR_LABEL = "init";

    public static final String INITIATOR_TO_OLD_LABEL = "itoo";

    public static final String INITIATOR_TO_CANDIDATE_LABEL = "itoc";


    /**
     * This active view stores {@link Node}s with which the local {@link Node} communicates directly,
     * in the scope of the overlaying application. In other words, the application can only send content
     * {@link message.Message}s with {@link Node}s in the active view.
     */
    private FixedSizeRandomSortedMap<Long, InetSocketAddress> activeView;

    /**
     * This passive view stores {@link Node}s with which the local {@link Node} communicates only through
     * control {@link Message}s. For example, it's in the passive peers that the local {@link Node} tries to
     * find candidates for optimizations, and to ensure network overlay connectivity
     */
    private FixedSizeRandomList<InetSocketAddress> passiveView;

    private XBotRoundPool currentRounds;

    private Map<InetSocketAddress, Consumer<CostNotification>> waitingForCosts;

    private static XBotNode xBotNode = new XBotNode();

    public static XBotNode xBotNode() {
        return xBotNode;
    }

    private XBotNode() {
        this(10);
    }

    private XBotNode(int initialCapacity) {
        super(initialCapacity);
        activeView = new FixedSizeRandomSortedMap<>(XBotSettings.activeViewSize());
        passiveView = new FixedSizeRandomList<>(XBotSettings.passiveViewSize());
        currentRounds = new XBotRoundPool();
        waitingForCosts = new HashMap<>();

        TimerTask optimizationTask = new TimerTask() {
            @Override
            public void run() {
                XBotNode.this.notify(new TimerNotification(XBotNode.this::tryOptimize));
            }
        };

        GlobalSettings.TIMER.schedule(optimizationTask, XBotSettings.optimizationPeriod(),
                XBotSettings.optimizationPeriod());
    }

    @Override
    public void handleMessage(Message message) {
        switch(message.messageType()) {
            case join:
                handleJoin(message);
                break;
            case acceptJoin:
                handleAcceptJoin(message);
                break;
            case forwardJoin:
                handleForwardJoin(message);
                break;
            case optimization:
                handleOptimization(message);
                break;
            case optimizationReply:
                handleOptimizationReply(message);
                break;
            case replace:
                handleReplace(message);
                break;
            case replaceReply:
                handleReplaceReply(message);
                break;
            case disconnect:
                handleDisconnect(message);
                break;
        }
    }

    @Override
    public void handleNotification(Notification notification) {
        if(notification instanceof MessageNotification)
            handleMessage(((MessageNotification) notification).message());
        else if(notification instanceof CostNotification)
            handleCost((CostNotification) notification);
    }

    @Override
    public void join(InetSocketAddress contactNode) {
        Message joinMessage = new Message(MessageDecoder.MessageType.join)
                .withSender()
                .setDestination(contactNode);
        sendMessage(joinMessage);
    }

    private void handleCost(CostNotification costNotification) {
        if(waitingForCosts.get(costNotification.node()) != null) {
            waitingForCosts.remove(costNotification.node()).accept(costNotification);
            return;
        }

        XBotRound disconnectedRound = currentRounds.round(XBotRound.Role.disconnected,
                XBotRound.Role.old, costNotification.node());
        if(disconnectedRound != null) {
            disconnectedRound.dtoc(activeView.getEntry(disconnectedRound.candidate()).getKey());
            disconnectedRound.dtoo(costNotification.cost());
            optimizeIf(disconnectedRound, XBotSettings::shouldOptimize);
            return;
        }

        XBotRound initiatorRound = currentRounds.initiatorRound();
        if(initiatorRound == null ||
                initiatorRound.candidate().equals(costNotification.node())) {
            if(GlobalSettings.DEBUGGING_LEVEL >= 3)
                System.out.println("Node received a cost but wasn't expecting one.");
            System.exit(1);
        }
        handleCandidateCost(costNotification);
    }

    private void handleCandidateCost(CostNotification costNotification) {
        InetSocketAddress candidate = costNotification.node();
        long cost = costNotification.cost();
        InetSocketAddress old = null;
        long oldCost = 0;
        Iterator<Map.Entry<Long, InetSocketAddress>> iterator =
                activeView.sortedIteratorFrom(XBotSettings.unbiasedViewSize());
        while(iterator.hasNext()) {
            Map.Entry<Long, InetSocketAddress> neighbour = iterator.next();
            long neighbourCost = neighbour.getKey();
            if(neighbourCost > cost) {
                old = neighbour.getValue();
                oldCost = neighbourCost;
                break;
            }
        }
        if(old == null) {
            if(GlobalSettings.DEBUGGING_LEVEL >= 4)
                System.out.println("Node couldn't find any optimization potential");
            currentRounds.removeInitiatorRound();
            return;
        }
        currentRounds.initiatorRound()
                .old(old)
                .itoc(cost)
                .itoo(oldCost);

        Message optimizationMessage = new Message(MessageDecoder.MessageType.optimization)
                .withSender()
                .setDestination(candidate)
                .addMetadataEntry(OLD_ADDRESS_LABEL, BBInetSocketAddress.toByteBuffer(old))
                .addMetadataEntry(INITIATOR_TO_OLD_LABEL, (ByteBuffer) ByteBuffer.allocate(Long.BYTES)
                        .putLong(oldCost).flip());
        sendMessage(optimizationMessage);
    }

    private void tryOptimize() {
        InetSocketAddress candidate = passiveView.getRandom();
        currentRounds.createRound(XBotRound.Role.initiator, candidate);
        RTTOracle.getRttOracle().getCost(candidate);
    }

    private void handleJoin(Message joinMessage) {
        InetSocketAddress sender = joinMessage.sender();
        RTTOracle.getRttOracle().getCost(sender);
        waitingForCosts.put(sender, this::finishJoin);

        activeView.forEach(neighbour -> {
            Message forwardJoinMessage = new Message(MessageDecoder.MessageType.forwardJoin)
                    .withSender()
                    .setDestination(neighbour)
                    .addMetadataEntry(JOINER_LABEL, BBInetSocketAddress.toByteBuffer(sender))
                    .addMetadataEntry(ACTIVE_RWL_LABEL, ByteBuffer.allocate(Short.BYTES).putShort(
                            XBotSettings.ACTIVE_RANDOM_WALK_LENGTH));
            sendMessage(forwardJoinMessage);
        });
    }

    private void handleAcceptJoin(Message acceptJoinMessage) {
        activeView.add(acceptJoinMessage.metadataEntry(JOIN_COST_LABEL).getLong(), acceptJoinMessage.sender());
    }

    private void finishJoin(CostNotification costNotification) {
        addToActiveView(costNotification.node(), costNotification.cost());

        Message acceptJoinMessage = new Message(MessageDecoder.MessageType.acceptJoin)
                .withSender()
                .setDestination(costNotification.node())
                .addMetadataEntry(JOIN_COST_LABEL, (ByteBuffer) ByteBuffer.allocate(Long.BYTES)
                        .putLong(costNotification.cost()).flip());
        sendMessage(acceptJoinMessage);
    }

    private void handleForwardJoin(Message forwardJoinMessage) {
        InetSocketAddress sender = forwardJoinMessage.sender();
        short arwl = forwardJoinMessage.metadataEntry(ACTIVE_RWL_LABEL).getShort();
        try {
            InetSocketAddress joiner = BBInetSocketAddress.fromByteBuffer(forwardJoinMessage.metadataEntry(JOINER_LABEL));
            if(arwl == XBotSettings.PASSIVE_RANDOM_WALK_LENGTH)
                addToPassiveView(joiner);
            if(activeView.size() == 1 || arwl == 0) {
                RTTOracle.getRttOracle().getCost(joiner);
                waitingForCosts.put(joiner, this::finishJoin);
                return;
            }
            InetSocketAddress forwardTo = activeView.getRandom();
            while(forwardTo.equals(sender))
                forwardTo = activeView.getRandom();
            Message newForwardJoinMessage = new Message(MessageDecoder.MessageType.forwardJoin)
                    .withSender()
                    .setDestination(forwardTo)
                    .addMetadataEntry(ACTIVE_RWL_LABEL, ByteBuffer.allocate(Short.BYTES).putShort((short) (arwl - 1)))
                    .addMetadataEntry(JOINER_LABEL, BBInetSocketAddress.toByteBuffer(joiner));
            sendMessage(newForwardJoinMessage);
        } catch (UnknownHostException e) {
            // TODO
            System.exit(1);
        }
    }

    private void handleOptimization(Message optimizationMessage) {
        InetSocketAddress sender = optimizationMessage.sender();
        if(!activeView.isFull()) {
            Message optimizationReply = new Message(MessageDecoder.MessageType.optimizationReply)
                    .withSender()
                    .setDestination(sender)
                    .addMetadataEntry(NO_DISCONNECT_LABEL, ByteBuffer.allocate(Byte.BYTES).put((byte) 1))
                    .addMetadataEntry(OPTIMIZATION_REPLY_LABEL, ByteBuffer.allocate(Byte.BYTES).put((byte) 1));
            sendMessage(optimizationReply);
            return;
        }
        FixedSizeRandomSortedMap.Entry<Long, InetSocketAddress> disconnectEntry =
                activeView.get(XBotSettings.unbiasedViewSize());
        InetSocketAddress disconnect = disconnectEntry.getValue();
        Message replaceMessage = new Message(MessageDecoder.MessageType.replace)
                .withSender()
                .setDestination(disconnect)
                .addMetadataEntry(OLD_ADDRESS_LABEL, optimizationMessage.metadataEntry(OLD_ADDRESS_LABEL))
                .addMetadataEntry(INITIATOR_TO_OLD_LABEL, optimizationMessage.metadataEntry(INITIATOR_TO_OLD_LABEL))
                .addMetadataEntry(INITIATOR_TO_CANDIDATE_LABEL,
                        (ByteBuffer) ByteBuffer.allocate(Long.BYTES).putLong(disconnectEntry.getKey()).flip())
                .addMetadataEntry(INITIATOR_LABEL, optimizationMessage.metadataEntry(Message.SENDER_LABEL));
        try {
            InetSocketAddress oldAddress = BBInetSocketAddress
                    .fromByteBuffer(optimizationMessage.metadataEntry(OLD_ADDRESS_LABEL));
            Message disconnectMessage = new Message(MessageDecoder.MessageType.swap)
                    .withSender()
                    .setDestination(oldAddress);
            // TODO
            sendMessage(disconnectMessage);
        } catch (UnknownHostException uhe) {
            // TODO
            uhe.printStackTrace();
            System.exit(1);
        }
        sendMessage(replaceMessage);
        try {
            currentRounds.createRound(XBotRound.Role.candidate,
                    sender,
                    BBInetSocketAddress.fromByteBuffer(optimizationMessage.metadataEntry(INITIATOR_TO_OLD_LABEL)),
                    disconnect)
                    .itoc(optimizationMessage.metadataEntry(INITIATOR_TO_CANDIDATE_LABEL).getLong())
                    .itoo(optimizationMessage.metadataEntry(INITIATOR_TO_OLD_LABEL).getLong());
            optimizationMessage.metadataEntry(INITIATOR_TO_CANDIDATE_LABEL).flip();
            optimizationMessage.metadataEntry(INITIATOR_TO_OLD_LABEL).flip();
        } catch(UnknownHostException uhe) {
            // TODO
            uhe.printStackTrace();
            System.exit(1);
        }
    }

    private void handleOptimizationReply(Message optimizationReplyMessage) {
        InetSocketAddress sender = optimizationReplyMessage.sender();
        XBotRound initiatorRound = currentRounds.initiatorRound();
        if(initiatorRound == null || !(initiatorRound.candidate().equals(sender))) {
            // TODO
            if(GlobalSettings.DEBUGGING_LEVEL >= 2)
                System.out.println("Optimization round went wrong on initiator, " +
                        "no info about candidate");
            System.exit(1);
        }
        boolean optimize = optimizationReplyMessage
                .metadataEntry(OPTIMIZATION_REPLY_LABEL).get() == 1;
        if(optimize) {
            disconnectNeighbour(initiatorRound.old());
            addToActiveView(initiatorRound.candidate(), initiatorRound.itoc());
        }
        currentRounds.removeInitiatorRound();
    }

    private void handleReplace(Message replaceMessage) {
        try {
            InetSocketAddress old = BBInetSocketAddress.fromByteBuffer(replaceMessage.metadataEntry(OLD_ADDRESS_LABEL));
            RTTOracle.getRttOracle().getCost(old);
            currentRounds.createRound(XBotRound.Role.disconnected,
                    BBInetSocketAddress.fromByteBuffer(replaceMessage.metadataEntry(INITIATOR_LABEL)),
                    replaceMessage.sender(),
                    old);
        } catch(UnknownHostException uhe) {
            // TODO
            uhe.printStackTrace();
            System.exit(1);
        }
    }

    private void handleReplaceReply(Message replaceReply) {
        InetSocketAddress sender = replaceReply.sender();
        XBotRound round = currentRounds.round(XBotRound.Role.candidate,
                XBotRound.Role.disconnected, sender);
        if(round == null) {
            if(GlobalSettings.DEBUGGING_LEVEL >= 3)
                System.out.println("Received wrong message: Replace Reply");
            return;
        }
        boolean optimize = replaceReply.metadataEntry(REPLACE_REPLY_LABEL).get() == 1;
        Message optimizationReply = new Message(MessageDecoder.MessageType.optimizationReply)
                .withSender()
                .setDestination(round.initiator())
                .addMetadataEntry(OPTIMIZATION_REPLY_LABEL,
                        ByteBuffer.allocate(Byte.BYTES).put((byte) (optimize ? 1 : 0)));
        sendMessage(optimizationReply);
        if(optimize) {
            if(round.initiator() == null || round.itoc() == 0) {
                // TODO
                if(GlobalSettings.DEBUGGING_LEVEL >= 2)
                    System.out.println("Optimization round went wrong on candidate, " +
                            "no info about initiator.");
                System.exit(1);
            }
            addToActiveView(round.initiator(), round.itoc());
            disconnectNeighbour(sender);
        }
        currentRounds.removeRound(XBotRound.Role.disconnected, round.initiator());
    }

    private void optimizeIf(XBotRound disconnectedRound, Function<XBotRound, Boolean> criteria) {
        // If this is true, the criteria passed and we want to optimize
        boolean optimize = criteria.apply(disconnectedRound);
        Message replaceReplyMessage = new Message(MessageDecoder.MessageType.replaceReply)
                .withSender()
                .setDestination(disconnectedRound.candidate())
                .addMetadataEntry(REPLACE_REPLY_LABEL, ByteBuffer.allocate(Byte.BYTES)
                        .put((byte) (optimize ? 1 : 0)));
        sendMessage(replaceReplyMessage);
        if(optimize) {
            if(disconnectedRound.old() == null || disconnectedRound.dtoo() == 0) {
                // TODO
                if(GlobalSettings.DEBUGGING_LEVEL >= 2)
                    System.out.println("Optimization round went wrong on disconnected, " +
                            "no info about old.");
                System.exit(1);
            }
            addToActiveView(disconnectedRound.old(), disconnectedRound.dtoo());
            disconnectNeighbour(disconnectedRound.candidate());
        }
        currentRounds.removeRound(XBotRound.Role.disconnected, disconnectedRound.initiator());
    }

    private void handleDisconnect(Message disconnectMessage) {

    }

    private void finishAddToActiveView(CostNotification costNotification) {
        addToActiveView(costNotification.node(), costNotification.cost());
    }

    private void addToActiveView(InetSocketAddress neighbour) {
        RTTOracle.getRttOracle().getCost(neighbour);
        waitingForCosts.put(neighbour, this::finishAddToActiveView);
    }

    private void addToActiveView(InetSocketAddress neighbour, long cost) {
        if(activeView.isFull())
            disconnectRandomNeighbour();
        activeView.add(cost, neighbour);
    }

    private void disconnectNeighbour(InetSocketAddress neighbour) {
        activeView.remove(neighbour);
        Message disconnectMessage = new Message(MessageDecoder.MessageType.disconnect)
                .withSender()
                .setDestination(neighbour);
        sendMessage(disconnectMessage);
        addToPassiveView(neighbour);
    }

    private InetSocketAddress disconnectRandomNeighbour() {
        InetSocketAddress disconnect = activeView.removeRandom();
        Message disconnectMessage = new Message(MessageDecoder.MessageType.disconnect)
                .withSender()
                .setDestination(disconnect);
        sendMessage(disconnectMessage);
        return addToPassiveView(disconnect);
    }

    private InetSocketAddress addToPassiveView(InetSocketAddress node) {
        if(passiveView.isFull())
            passiveView.removeRandom();
        passiveView.add(node);
        return node;
    }

    private void sendMessage(Message message) {
        TCP.tcp().notify(new MessageNotification(message));
    }
}
