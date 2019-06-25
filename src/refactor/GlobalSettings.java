package refactor;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This interface just stores important settings that are used throughout the whole protocol implementation
 * It serves as a kind of utility to avoid inconsistencies
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 20.06.2019
 */
public class GlobalSettings {

    /**
     * This field stores the local address of the local node, so it can be accessed throughout the whole program.
     * It can be set and got at will, but is assumed to only be set once, at the beginning of execution
     */
    public static InetSocketAddress LOCAL_ADDRESS;

    /**
     * Maximum time, in milliseconds, a thread can block in order to try to connect a node to another one. After this
     * time, an exception will be thrown and the connection between the nodes will be assumed to have failed
     */
    public static final int CONNECT_TIMEOUT = 10 * 1000;

    /**
     * This is the first global Thread pool of the program. It is responsible for flexible tasks, such as sending pings
     * with {@link protocol.oracle.Oracle}s and such tasks. It seems logical to have the whole implementation use the
     * same Thread pools but this can be changed in the future, if needed
     */
    public static final ExecutorService FLEX_THREAD_POOL = Executors.newCachedThreadPool();

    /**
     * This is the second global Thread pool. It is responsible for the few fixed Threads the program has, like the
     * Threads of the {@link refactor.network.TCP} layer (receiving and sending), etc. The number of Threads is fixed
     * and depends on the implementation
     */
    public static final ExecutorService FIXED_THREAD_POOL = Executors.newFixedThreadPool(3);

    /**
     * Keeps information on what the current implementation charset is, for encoding messages
     */
    public static final Charset CHARSET = StandardCharsets.UTF_16;

    /**
     * Maximum size of a message, including metadata and data, in bytes.
     * Messages can have metadata of varying lengths, and most of the time, they will either have
     * a lot of metadata fields or a lot of data, but most likely never both, so having only a maximum
     * size for the whole message seems to be the most practical solution, or else resources would be wasted.
     * The protocol will read messages in blocks (at max) of this size
     */
    public static final int MAX_MESSAGE_SIZE = 1024 * 400;

    /**
     * Maximum size of the labels of the entries of metadata, in bytes. This is to somewhat standardize serialization
     * across implementations
     */
    public static final int MAX_LABEL_SIZE = 32;

    /**
     * Define what level of debugging logs is wanted for each run of the program.
     * {@code 0} means no debug logs at all.
     * {@code 1} means only the most important (things that can break the system, but somehow are not caught by exceptions)
     * {@code 2} means all logs that can be potentially harmful, but may be OK
     * {@code 3} means all logs the program currently has
     */
    public static final byte DEBUGGING_LEVEL = 2;
}
