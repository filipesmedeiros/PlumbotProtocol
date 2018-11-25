package protocol.oracle;

import interfaces.MessageListener;
import interfaces.OracleUser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

public interface Oracle extends MessageListener {

    void init();

    void getCost(InetSocketAddress dest)
        throws IOException, InterruptedException;

    void setUser(OracleUser user);

    void setUsers(Set<OracleUser> users);

    void setCostTTL(long ttl);
}
