package interfaces;

import protocol.PeerSamplingNode;
import protocol.oracle.Oracle;

public interface OptimizerNode extends OracleUser, PeerSamplingNode {

    void setOracle(Oracle oracle);

    void setInterval(long interval);
}