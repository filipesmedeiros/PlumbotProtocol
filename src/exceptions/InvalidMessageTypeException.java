package exceptions;

import nettyFoutoRefactor.network.messaging.Message;
import nettyFoutoRefactor.network.messaging.MessageSerializer;

/**
 * Exception that is thrown when the program tries to instantiate a {@link Message}
 * with a type that is not correct (not present in {@link MessageSerializer}'s type list
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 21.06.2019
 */
public class InvalidMessageTypeException extends Exception {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "Invalid message type. Type was: ";

    /**
     * Default constructor of this exception, takes the type which the program tried to use
     * @param number The type the program tried to use for the {@link Message}
     */
    public InvalidMessageTypeException(byte number) {
        super(MESSAGE + number);
    }
}
