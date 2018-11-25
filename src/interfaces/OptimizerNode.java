package interfaces;

import protocol.oracle.Oracle;

public interface OptimizerNode {

    void setOracle(Oracle oracle);

    void setInterval(long interval);
}