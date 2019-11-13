package plumtree;

import network.Host;

public class IHaveAnnouncement {

    private Host sender;
    private int round;

    public IHaveAnnouncement(Host sender, int round) {
        this.sender = sender;
        this.round = round;
    }

    public Host sender() {
        return sender;
    }

    public int round() {
        return round;
    }
}
