package exceptions;

import nettyFoutoRefactor.network.messaging.Message;
import common.GlobalSettings;

/**
 * Exception that is thrown when the program tries to create a {@link Message} whose total
 * size in bytes exceeds that which is defined in {@link GlobalSettings}
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 21.06.2019
 */
public class MessageTooLargeException extends Exception {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "Trying to create Message that is too large. Message size in bytes was: ";

    /**
     * Default constructor of this exception, takes the size of the {@link Message} which was
     * created, but failed
     * @param size The size, in bytes, of the {@link Message}
     */
    public MessageTooLargeException(int size) {
        super(MESSAGE + size);
    }
}
