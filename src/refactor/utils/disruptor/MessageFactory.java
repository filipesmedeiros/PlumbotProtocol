package refactor.utils.disruptor;

import com.lmax.disruptor.EventFactory;

import refactor.message.Message;

public class MessageFactory implements EventFactory<Message> {

	@Override
	public Message newInstance() {
		return new Message(null);
	}

}
