package interfaces;

import protocol.oracle.TimeCostOracle;

import java.net.InetSocketAddress;

public interface OracleUser {

    void notifyCost(TimeCostOracle.CostNotification noti);

    InetSocketAddress id();
}
