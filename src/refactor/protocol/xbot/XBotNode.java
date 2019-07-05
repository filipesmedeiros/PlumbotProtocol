package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import refactor.GlobalSettings;
import refactor.message.Message;
import refactor.utils.AbstractNotificationListener;
import refactor.protocol.Node;
import refactor.protocol.oracle.AsyncOracle;
import refactor.utils.MessageNotification;
import refactor.utils.RandomList;
import refactor.utils.Notification;
import refactor.utils.TimerNotification;

/**
 * To know more about the XBot protocol, read http://asc.di.fct.unl.pt/~jleitao/pdf/srds09-leitao.pdf
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 27.06.2019
 */
public class XBotNode extends AbstractNotificationListener implements Node{

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

	private BlockingQueue<Notification> notifications;

	public XBotNode() {
		activeView = new RandomList<>();
		passiveView = new RandomList<>();
		currentRounds = new XBotRoundPool();

		GlobalSettings.FIXED_THREAD_POOL.submit(this::handleNotifications);

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

	@Override
	public void notify(Notification notification) {
		try {
			notifications.put(notification);
		} catch (InterruptedException e) {
			// TODO
			System.exit(1);
		}
	}

	@Override
	public void handleNotifications() {
		for(;;) {
			try {
				Notification notification = notifications.take();
				if(notification instanceof TimerNotification)
					((TimerNotification) notification).runTask();
				else
					handleMessage(((MessageNotification) notification).message());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void tryOptimize() {

	}

	private void handleMessage(Message message) {

	}
}
