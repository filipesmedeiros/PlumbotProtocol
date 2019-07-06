package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.util.TimerTask;

import refactor.GlobalSettings;
import refactor.message.Message;
import refactor.protocol.AbstractNode;
import refactor.protocol.notifications.Notification;
import refactor.protocol.notifications.TimerNotification;
import refactor.utils.*;
import refactor.protocol.Node;
import refactor.protocol.oracle.AsyncOracle;

/**
 * To know more about the XBot protocol, read http://asc.di.fct.unl.pt/~jleitao/pdf/srds09-leitao.pdf
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 27.06.2019
 */
public class XBotNode extends AbstractNode {

	/**
	 * This active view stores {@link Node}s with which the local {@link Node} communicates directly,
	 * in the scope of the overlaying application. In other words, the application can only send content
	 * {@link message.Message}s with {@link Node}s in the active view.
	 */
	private RandomList<InetSocketAddress> activeView;

	/**
	 * This passive view stores {@link Node}s with which the local {@link Node} communicates only through
	 * control {@link Message}s. For example, it's in the passive peers that the local {@link Node} tries to
	 * find candidates for optimizations, and to ensure network overlay connectivity
	 */
	private RandomList<InetSocketAddress> passiveView;

	private XBotRoundPool currentRounds;

	private AsyncOracle oracle;

	private static XBotNode xBotNode = new XBotNode();

	public static XBotNode xBotNode() {
		return xBotNode;
	}

	private XBotNode() {
		this(10);
	}

	private XBotNode(int initialCapacity) {
		super(initialCapacity);
		activeView = new RandomList<>();
		passiveView = new RandomList<>();
		currentRounds = new XBotRoundPool();

		GlobalSettings.FIXED_THREAD_POOL.submit(this::takeNotification);

		TimerTask optimizationTask = new TimerTask() {
			@Override
			public void run() {
				triggerOptimizationRound();
			}
		};

		GlobalSettings.TIMER.schedule(optimizationTask, XBotSettings.optimizationPeriod(),
				XBotSettings.optimizationPeriod());
	}

	private void triggerOptimizationRound() {
		notifications.add(new TimerNotification(this::tryOptimize));
	}

	private void tryOptimize() {
		InetSocketAddress chosen = passiveView.getRandom();
		oracle.getCost(chosen);
	}

	@Override
	protected void handleMessage(Message message) {

	}

	@Override
	public void handleNotification(Notification notification) {

	}
}
