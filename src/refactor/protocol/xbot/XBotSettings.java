package refactor.protocol.xbot;

import nettyFoutoRefactor.network.messaging.Message;
import refactor.GlobalSettings;
import refactor.exception.IllegalSettingChangeException;

/**
 * This interface just stores important settings that are associated with the XBot protocol, like the size of
 * the {@link refactor.protocol.Node}'s views and certain timeouts
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 29.06.2019
 */
public class XBotSettings {

    /**
     * This field is used to set the size of the active view of the local {@link refactor.protocol.Node}. Changing this
     * setting mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    private static short ACTIVE_VIEW_SIZE = 7;

    /**
     * Simple method that just returns the set active view size
     * @return The size of this Peer's active view (XBot-wise)
     */
    public static short activeViewSize() {
    	return ACTIVE_VIEW_SIZE;
    }

    /**
     * Simple method that sets the size of the active view, given that the execution of the protocol hasn't started
     * @param size The size of the active view of the {@link refactor.protocol.Node}
     * @throws IllegalSettingChangeException Thrown if the execution of the protocol has begun
     */
    public static void setActiveViewSize(short size)
    		throws IllegalSettingChangeException {
    	if(GlobalSettings.areSettingsLocked())
    		throw new IllegalSettingChangeException("Active View Size");
    	ACTIVE_VIEW_SIZE = size;
    }

    /**
     * This field is used to set the number of active neighbours of this {@link refactor.protocol.Node} that are
     * unbiased. In other words, they are protected from being swapped out of its active view. Changing this setting
     * mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    private static short UNBIASED_VIEW_SIZE = 2;

    /**
     * Simple method that just returns the set number of unbiased neighbours
     * @return The number of unbiased neighbours this {@link refactor.protocol.Node} has
     */
    public static short unbiasedViewSize() {
        return UNBIASED_VIEW_SIZE;
    }

    /**
     * Simple method that sets the number of unbiased neighbours this {@link refactor.protocol.Node} has, given that the
     * execution of the protocol hasn't started
     * @param size The number of unbiased neighbours of the {@link refactor.protocol.Node}
     * @throws IllegalSettingChangeException Thrown if the execution of the protocol has begun
     */
    public static void setUnbiasedViewSize(short size)
            throws IllegalSettingChangeException {
        if(GlobalSettings.areSettingsLocked())
            throw new IllegalSettingChangeException("Unbiased View Size");
        UNBIASED_VIEW_SIZE = size;
    }
    
    /**
     * This field is used to set the size of the passive view of the local {@link refactor.protocol.Node}. Changing this
     * setting mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    public static short PASSIVE_VIEW_SIZE = 10;
    
    /**
     * Simple method that just returns the set passive view size
     * @return The size of this Peer's passive view (XBot-wise)
     */
    public static short passiveViewSize() {
    	return PASSIVE_VIEW_SIZE;
    }
    
    /**
     * Simple method that sets the size of the passive view, given that the execution of the protocol hasn't started
     * @param size The size of the passive view of the {@link refactor.protocol.Node}
     * @throws IllegalSettingChangeException Thrown if the execution of the protocol has begun
     */
    public static void setPassiveViewSize(short size)
    		throws IllegalSettingChangeException {
    	if(GlobalSettings.areSettingsLocked())
    		throw new IllegalSettingChangeException("Passive View Size");
    	PASSIVE_VIEW_SIZE = size;
    }

    /**
     * This value defines how many times a Join is spread across the network, through JoinForward
     * {@link Message}s
     */
    public static short ACTIVE_RANDOM_WALK_LENGTH = 4;

    /**
     * This value defines at which point during the JoinForward spread, the {@link refactor.protocol.Node} is added to a
     * passive view. Note that, since this is implemented with the help of the {@code ACTIVE_RANDOM_WALK_LENGTH}, this
     * value actually represents how close to {@code 0} you have to be to do it, i.e. an ARWL of 10 and a PRWL of 3
     * means that on the 7th "spread", the Peer will be added to a passive view
     */
    public static short PASSIVE_RANDOM_WALK_LENGTH = 2;
    
    /**
     * This field is used to set the time between optimization rounds that this {@link refactor.protocol.Node} starts,
     * in milliseconds. So, if this field has value 2.400.000, this {@link refactor.protocol.Node} will try to optimize
     * the network every 40 minutes.
     */
    public static long OPTIMIZATION_PERIOD = 1000 * 60 * 20; // 20 minutes
    
    /**
     * Simple method that just returns the set optimization period
     * @return The optimization period, in milliseconds
     */
    public static long optimizationPeriod() {
    	return OPTIMIZATION_PERIOD;
    }

    /**
     * This field represents the minimum time the protocol has to wait for this {@link refactor.protocol.Node} to be
     * able to ping the same neighbour twice, in seconds, i.e. if its value is 30, pinging the same neighbour within
     * 30 seconds does nothing
     */
    public static long ORACLE_PING_INTERVAL = 30;

    /**
     * This fields represents how much time the protocol will wait for a response to a ping to a neighbour, in seconds,
     * i.e. if the value is 60, after 60 seconds, the protocol will assume the neighbour will never respond. This should
     * have implications on the overlay, such as, if enough nodes assume a neighbour is non-responsive, it is removed
     * from the overlay
     */
    public static long ORACLE_PING_TIMEOUT = 60;
    
    /**
     * Simple method that sets the optimization period, given that the execution of the protocol hasn't started
     * @param period The optimization period of the {@link refactor.protocol.Node}
     * @throws IllegalSettingChangeException Thrown if the execution of the protocol has begun
     */
    public static void setOptimizationPeriod(long period)
    		throws IllegalSettingChangeException {
    	if(GlobalSettings.areSettingsLocked())
    		throw new IllegalSettingChangeException("Optimization Period");
    	OPTIMIZATION_PERIOD = period;
    }
    
    /**
     * Utility method for the protocol to check, before execution, that all settings set by the overlaying application
     * are correct and can the program can run properly
     * @return {@code true} if settings are valid (ready for execution)
     */
    public static boolean isValid() {
    	return ACTIVE_VIEW_SIZE > 0 && PASSIVE_VIEW_SIZE > 0 && OPTIMIZATION_PERIOD > 0;
    }

    /**
     * This function represents the criteria used to evaluate if an optimization round should turn into an actual
     * optimization, and can be changed according to the app's needs
     * @param optimizationRound The optimization round being evaluated
     * @return True if the protocol should optimize the network, using this round's info
     */
    public static boolean shouldOptimize(XBotRound optimizationRound) {
        long itoo = optimizationRound.itoo();
        long dtoc = optimizationRound.dtoc();
        long itoc = optimizationRound.itoc();
        long dtoo = optimizationRound.dtoo();
        return itoo + dtoc > itoc + dtoo;
    }
}
