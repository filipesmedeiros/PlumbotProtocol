package refactor.exception;

import java.io.IOException;

/**
 * Exception that is thrown when the program tries to communicate with a {@link refactor.protocol.Node} with which
 * it has no established connection (yet, probably)
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 22.06.2019
 */
public class ConnectionNotEstablishedException extends IOException {

    /**
     * Basic message to display on the console
     */
    private static final String MESSAGE = "These two nodes have no established connection";

    /**
     * Default constructor of this exception
     */
    public ConnectionNotEstablishedException() {
        super(MESSAGE);
    }
}
