package interfaces;

public interface CostComparer {

    // I want to do this JavaDoc thing everywhere

    /**
     * Has to return true if optimization is the right choice.
     * Implementations may be more or less strict on their definition of "right choice"
     * @param itoc Initiator to candidate cost
     * @param itoo Initiator to old cost
     * @param ctod Candidate to disconnect cost
     * @param dtoc Disconnect to candidate cost
     * @return True if nodes should optimize, false otherwise
     */
    boolean compare(long itoc, long itoo, long ctod, long dtoc);
}
