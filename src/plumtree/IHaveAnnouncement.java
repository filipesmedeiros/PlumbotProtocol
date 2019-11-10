package plumtree;

import java.net.InetSocketAddress;

public class IHaveAnnouncement {

    private InetSocketAddress sender;
    private int round;

    public IHaveAnnouncement(InetSocketAddress sender, int round) {
        this.sender = sender;
        this.round = round;
    }

    public InetSocketAddress sender() {
        return sender;
    }

    public int round() {
        return round;
    }
}
