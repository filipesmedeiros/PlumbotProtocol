package interfaces;

import protocol.PeerSamplingNode;
import protocol.oracle.Oracle;

public interface OptimizerNode extends OracleUser, PeerSamplingNode {

    void setOracle(Oracle oracle) throws IllegalArgumentException;

    void setPeriod(long period) throws IllegalArgumentException;
}