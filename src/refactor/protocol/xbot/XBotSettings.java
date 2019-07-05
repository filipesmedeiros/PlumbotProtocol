package refactor.protocol.xbot;

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
    private static short ACTIVE_VIEW_SIZE;
    
    /**
     * Simple method that just returns the set active view size
     * @return The size of this node's active view (XBot-wise)
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
     * This field is used to set the size of the passive view of the local {@link refactor.protocol.Node}. Changing this
     * setting mid-execution is permitted but discouraged and can lead to incorrect behaviors and errors
     */
    public static short PASSIVE_VIEW_SIZE;
    
    /**
     * Simple method that just returns the set passive view size
     * @return The size of this node's passive view (XBot-wise)
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
     * This field is used to set the time between optimization rounds that this {@link refactor.protocol.Node} starts,
     * in milliseconds. So, if this field has value 2.400.000, this {@link refactor.protocol.Node} will try to optimize
     * the network every 40 minutes.
     */
    public static long OPTIMIZATION_PERIOD;
    
    /**
     * Simple method that just returns the set optimization period
     * @return The optimization period, in milliseconds
     */
    public static long optimizationPeriod() {
    	return OPTIMIZATION_PERIOD;
    }
    
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
    public boolean isValid() {
    	return ACTIVE_VIEW_SIZE > 0 && PASSIVE_VIEW_SIZE > 0 && OPTIMIZATION_PERIOD > 0;
    }

}
