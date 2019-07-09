package refactor.protocol.notifications;

import refactor.GlobalSettings;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractNotifiable implements Notifiable {

	protected BlockingQueue<Notification> notifications;

	public AbstractNotifiable() {
		this(10);
	}

	public AbstractNotifiable(int initialCapacity) {
		notifications = new ArrayBlockingQueue<>(initialCapacity);
		System.out.println("Spawning Thread for " + this.getClass().getSimpleName() + "'s notification listening Thread.");
		GlobalSettings.FIXED_THREAD_POOL.submit(this::takeNotification);
	}

	@Override
	public void notify(Notification notification) {
		try {
			notifications.put(notification);
		} catch(InterruptedException ie) {
			// TODO
			System.exit(1);
		}
	}

	@Override
	public void takeNotification() {
		for(;;) {
			try {
				Notification notification = notifications.take();
				handleNotification(notification);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
