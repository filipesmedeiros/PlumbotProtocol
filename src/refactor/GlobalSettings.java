package refactor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import refactor.exception.IllegalSettingChangeException;
import refactor.utils.BBInetSocketAddress;

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
     * It can be set and got at will, but is assumed to only be set once, at the beginning of execution. It is
     * assumed also to be an address that all {@link refactor.protocol.Node}s can see and connect to
     */
    private static InetSocketAddress LOCAL_ADDRESS;

    /**
     * Same as the local address field, but stored as a {@link ByteBuffer} for convenience purposes
     */
    private static ByteBuffer LOCAL_ADDRESS_BYTES;

    /**
     * This field if used as a flag representing that the execution of the protocol in this
     * {@link refactor.protocol.Node} has started, so some setting cannot be changed
     */
    private static boolean SETTINGS_LOCKED = false;
    
    /**
     * This method is called when the execution of the protocol starts, and it locks certain settings from being
     * changed from there on out
     * @throws IllegalSettingChangeException If the protocol or the app try to lock the settings, but they are invalid
     */
    public static void lockSettings()
            throws IllegalSettingChangeException {
        if(!isValid())
            throw new IllegalSettingChangeException("Can't lock invalid settings.");
    	SETTINGS_LOCKED = true;
    	LOCAL_ADDRESS_BYTES = BBInetSocketAddress.localToByteBuffer();
    }
    
    /**
     * This method simply returns the {@code boolean} field {@code SETTINGS_LOCKED}
     * @return The boolean field {@code SETTINGS_LOCKED}, which indicates if certain settings can be changed
     */
    public static boolean areSettingsLocked() {
    	return SETTINGS_LOCKED;
    }
    
    /**
     * Simple method to get the previously set local address.
     * @return The local address of this {@link refactor.protocol.Node}
     */
    public static InetSocketAddress localAddress() {
    	return LOCAL_ADDRESS;
    }

    /**
     * Simple method to get the previously set local address, as a {@link ByteBuffer}.
     * @return The local address of this {@link refactor.protocol.Node}, in the form of a {@link ByteBuffer}
     */
    public static ByteBuffer localAddressBytes() {
        return LOCAL_ADDRESS_BYTES;
    }
    
    /**
     * Simple method to set the local address of this {@link refactor.protocol.Node}
     * @param localAddress The local address to define as the address of the local {@link refactor.protocol.Node}
     * @throws IllegalSettingChangeException If the program or the overlaying app try to change this setting after
     * execution has begun
     */
    public static void setLocalAddress(InetSocketAddress localAddress)
    		throws IllegalSettingChangeException {
    	if(SETTINGS_LOCKED)
    		throw new IllegalSettingChangeException("Local Address");
    	LOCAL_ADDRESS = localAddress;
    }

    /**
     * Maximum time, in milliseconds, a thread can block in order to try to connect a node to another one. After this
     * time, an exception will be thrown and the connection between the nodes will be assumed to have failed. Changing
     * this setting mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    public static int CONNECT_TIMEOUT = 10 * 1000;

    /**
     * This is the first global Thread pool of the program. It is responsible for flexible tasks, such as sending pings
     * with {@link protocol.oracle.Oracle}s and such tasks. It seems logical to have the whole implementation use the
     * same Thread pools but this can be changed in the future, if needed
     */
    public static final ExecutorService FLEX_THREAD_POOL = Executors.newCachedThreadPool();
    
    /**
     * This is the second global {@link Thread} pool. It is responsible for the few fixed Threads the program has, like
     * the Threads of the {@link refactor.network.TCP} layer (receiving and sending), etc. The number of {@link Thread}s
     * is fixed and depends on the implementation
     */
    public static final ExecutorService FIXED_THREAD_POOL = Executors.newFixedThreadPool(3);

    /**
     * This is the single {@link Timer} used to manage all the periodic tasks the protocol has to execute. To use it,
     * the program adds {@link java.util.TimerTask}s to this timer. They should complete quickly, as to not block other
     * tasks in the timer
     */
    public static final Timer TIMER = new Timer();

    /**
     * Keeps information on what the current implementation charset is, for encoding messages. Changing this
     * setting mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    public static Charset CHARSET = StandardCharsets.UTF_16;

    /**
     * Maximum size of a message, including metadata and data, in bytes.
     * Messages can have metadata of varying lengths, and most of the time, they will either have
     * a lot of metadata fields or a lot of data, but most likely never both, so having only a maximum
     * size for the whole message seems to be the most practical solution, or else resources would be wasted.
     * The protocol will read messages in blocks (at max) of this size. Changing this setting mid-execution
     * is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    public static int MAX_MESSAGE_SIZE = 1024 * 400;

    /**
     * Maximum size of the labels of the entries of metadata, in bytes. This is to somewhat standardize serialization
     * across implementations. Changing this setting mid-execution is permitted but discouraged
     */
    public static int MAX_LABEL_SIZE = 32;

    /**
     * Define what level of debugging logs is wanted for each run of the program.
     * {@code 0} means no debug logs at all.
     * {@code 1} means only the most important (things that can break the system, but somehow are not caught by exceptions)
     * {@code 2} means all logs that can be potentially harmful, but may be OK
     * {@code 3} means all logs the program currently has
     */
    public static byte DEBUGGING_LEVEL = 2;
    
    /**
     * Utility method for the protocol to check, before execution, that all settings set by the overlaying application
     * are correct and can the program can run properly
     * @return {@code True} if the settings are valid (ready for execution)
     */
    public static boolean isValid() {
    	return LOCAL_ADDRESS != null && MAX_MESSAGE_SIZE > 0
    			&& MAX_LABEL_SIZE > 0 && CONNECT_TIMEOUT > 0;
    }
}
