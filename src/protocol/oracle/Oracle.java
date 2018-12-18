package protocol.oracle;

import interfaces.Notifiable;
import interfaces.NetworkNotifiable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

public interface Oracle extends NetworkNotifiable {

    void init();

    void getCost(InetSocketAddress dest)
        throws IOException, InterruptedException;

    void setUser(Notifiable user);

    void setUsers(Set<Notifiable> users);

    void setCostTTL(long ttl);
}
