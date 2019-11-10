package exceptions;

import nettyFoutoRefactor.network.messaging.Message;

/**
 * Exception that is thrown when the program tries to process (decode, encode, etc) a {@link Message}
 * object that is either {@code null} or empty (no metadata, no data)
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 21.06.2019
 */
public class NullMessageException extends Exception {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "Null message.";

    /**
     * Default constructor of this exception
     */
    public NullMessageException() {
        super(MESSAGE);
    }
}
