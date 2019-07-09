package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

import refactor.GlobalSettings;
import refactor.exception.SingletonIsNullException;
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

	public static final String OLD_COST_LABEL = "oldc";

	public static final String JOINER_LABEL = "jnr";

	public static final String ACTIVE_RWL_LABEL = "arwl";

	public static final String JOIN_COST_LABEL = "jnc";

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

	private Set<InetSocketAddress> joiningMe;

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
		joiningMe = new HashSet<>();

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
		try {
			TCP.tcp().notify(new MessageNotification(joinMessage));
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
	}

	private void handleCost(CostNotification costNotification) {
		if(joiningMe.contains(costNotification.sender())) {
			finishJoin(costNotification);
			joiningMe.remove(costNotification.sender());
			return;
		}

		InetSocketAddress candidate = costNotification.sender();
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
		if(old == null)
			return;
		currentRounds.createRound(XBotRound.Role.initiator, candidate)
				.addRole(XBotRound.Role.candidate, candidate)
				.addRole(XBotRound.Role.old, old);

		Message optimizationMessage = new Message(MessageDecoder.MessageType.optimization)
				.withSender()
				.setDestination(candidate)
				.addMetadataEntry(OLD_ADDRESS_LABEL, BBInetSocketAddress.toByteBuffer(old))
				.addMetadataEntry(OLD_COST_LABEL, ByteBuffer.allocate(Long.BYTES).putLong(oldCost));
		try {
			TCP.tcp().notify(new MessageNotification(optimizationMessage));
		} catch(SingletonIsNullException sine) {
			// TODO
			System.exit(1);
		}
	}

	private void tryOptimize() {
		InetSocketAddress candidate = passiveView.getRandom();
		RTTOracle.getRttOracle().getCost(candidate);
	}

	private void handleJoin(Message joinMessage) {
		InetSocketAddress sender = joinMessage.sender();
		RTTOracle.getRttOracle().getCost(sender);
		joiningMe.add(sender);

		try {
			TCP tcp = TCP.tcp();
			activeView.forEach(neighbour -> {
				Message forwardJoinMessage = new Message(MessageDecoder.MessageType.forwardJoin)
						.withSender()
						.setDestination(neighbour)
						.addMetadataEntry(JOINER_LABEL, BBInetSocketAddress.toByteBuffer(sender))
						.addMetadataEntry(ACTIVE_RWL_LABEL, ByteBuffer.allocate(Short.BYTES).putShort(
								XBotSettings.ACTIVE_RANDOM_WALK_LENGTH));
				tcp.notify(new MessageNotification(forwardJoinMessage));
			});
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
	}

	private void handleAcceptJoin(Message acceptJoinMessage) {
		activeView.add(acceptJoinMessage.metadataEntry(JOIN_COST_LABEL).getLong(), acceptJoinMessage.sender());
	}

	private void finishJoin(CostNotification costNotification) {
		if(activeView.isFull())
			disconnectRandomNeighbour();
		activeView.add(costNotification.cost(), costNotification.sender());

		Message acceptJoinMessage = new Message(MessageDecoder.MessageType.acceptJoin)
				.withSender()
				.setDestination(costNotification.sender())
				.addMetadataEntry(JOIN_COST_LABEL, ByteBuffer.allocate(Long.BYTES).putLong(costNotification.cost()));
		try {
			TCP.tcp().notify(new MessageNotification(acceptJoinMessage));
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
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
				joiningMe.add(joiner);
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
			TCP.tcp().notify(new MessageNotification(newForwardJoinMessage));
		} catch (UnknownHostException | SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}

	}

	private InetSocketAddress disconnectRandomNeighbour() {
		InetSocketAddress disconnect = activeView.removeRandom();
		Message disconnectMessage = new Message(MessageDecoder.MessageType.disconnect)
				.withSender();
		try {
			TCP.tcp().notify(new MessageNotification(disconnectMessage));
		} catch (SingletonIsNullException e) {
			// TODO
			System.exit(1);
		}
		return addToPassiveView(disconnect);
	}

	private InetSocketAddress addToPassiveView(InetSocketAddress node) {
		if(passiveView.isFull())
			passiveView.removeRandom();
		passiveView.add(node);
		return node;
	}
}
