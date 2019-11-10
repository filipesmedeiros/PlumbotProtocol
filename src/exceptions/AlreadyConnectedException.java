package exceptions;

import java.io.IOException;

/**
 * Exception that is thrown when the program tries to establish a connect between two {@link refactor.protocol.Node}s
 * that already have an established connection. This is not bad per se, its just a way to catch this situation
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 22.06.2019
 */
public class AlreadyConnectedException extends IOException {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "These two nodes already have an established connection";

    /**
     * Default constructor of this exception
     */
    public AlreadyConnectedException() {
        super(MESSAGE);
    }
}
