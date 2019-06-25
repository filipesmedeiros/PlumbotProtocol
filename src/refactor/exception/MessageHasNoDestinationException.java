package refactor.exception;

import java.io.IOException;

/**
 * Exception that is thrown when the program tries to send a {@link refactor.message.Message} through the network,
 * but said {@link refactor.message.Message} has no defined destination
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 22.06.2019
 */
public class MessageHasNoDestinationException extends IOException {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "Message is trying to be sent but has no destination";

    /**
     * Default constructor of this exception
     */
    public MessageHasNoDestinationException() {
        super(MESSAGE);
    }
}
