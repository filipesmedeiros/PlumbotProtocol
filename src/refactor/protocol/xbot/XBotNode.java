package refactor.protocol.xbot;

import java.net.InetSocketAddress;

import refactor.GlobalSettings;
import refactor.message.Message;
import refactor.protocol.AbstractMessageListener;
import refactor.protocol.Node;
import refactor.protocol.oracle.AsyncOracle;
import refactor.utils.RandomHashSet;

/**
 * To know more about the XBot protocol, read {@link http://asc.di.fct.unl.pt/~jleitao/pdf/srds09-leitao.pdf}
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 27.06.2019
 */
public class XBotNode extends AbstractMessageListener implements Node{

	/**
	 * This active view stores {@link Node}s with which the local {@link Node} communicates directly,
	 * in the scope of the overlaying application. In other words, the application can only send content
	 * {@link message.Message}s with {@link Node}s in the active view.
	 */
	private RandomHashSet<InetSocketAddress> activeView;

	/**
	 * This passive view stores {@link Node}s with which the local {@link Node} communicates only through
	 * control {@link Message}s. For example, it's in the passive peers that the local {@link Node} tries to
	 * find candidates for optimizations, and to ensure network overlay connectivity
	 */
	private RandomHashSet<InetSocketAddress> passiveView;

	private XBotRoundPool currentRounds;

	private AsyncOracle oracle;

	public XBotNode() {
		activeView = new RandomHashSet<>();
		passiveView = new RandomHashSet<>();
		currentRounds = new XBotRoundPool();

		GlobalSettings.FIXED_THREAD_POOL.submit(this::triggerOptimizationRound);
	}

	public void triggerOptimizationRound() {
		for(;;) {
			InetSocketAddress chosen = passiveView.getRandom();
			oracle.getCost(chosen);

			try {
				Thread.currentThread().wait(XBotSettings.OPTIMIZATION_PERIOD);
			} catch (InterruptedException e) {
				// TODO
				System.exit(1);
			}
		}
	}

	@Override
	public void notify(Message message) {

	}
}
