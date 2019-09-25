package refactor.framework;

import java.util.LinkedList;
import java.util.List;

public class DummyEdgeProtocolStack implements ProtocolStack {

    private List<Protocol> protocols;
    private Protocol dummyEdgeProtocol;

    public DummyEdgeProtocolStack() {
        protocols = new LinkedList<>();
        dummyEdgeProtocol = new EdgeDummyProtocol();
    }

    @Override
    public Protocol topProtocol() {
        return protocols.get(protocols.size() - 1);
    }

    @Override
    public Protocol bottomProtocol() {
        return protocols.get(0);
    }

    @Override
    public Protocol protocol(int index) {
        return protocols.get(index + 1);
    }

    @Override
    public Protocol addProtocol(Protocol protocol, int index) {
        if(index == 0)
            return addBottomProtocol(protocol);
        if(index == protocols.size() - 1)
            return addTopProtocol(protocol);
        Protocol oldProtocol = protocols.get(index);
        protocols.add(index, protocol);
        Protocol downProtocol = oldProtocol.downProtocol();
        downProtocol.upProtocol(protocol);
        protocol.downProtocol(downProtocol);
        protocol.upProtocol(oldProtocol);
        oldProtocol.downProtocol(protocol);
        return oldProtocol;
    }

    @Override
    public Protocol addProtocolOver(Protocol thisProtocol, Protocol overThisProtocol) {
        Protocol oldProtocol = overThisProtocol.upProtocol();
        oldProtocol.downProtocol(thisProtocol);
        overThisProtocol.upProtocol(thisProtocol);
        thisProtocol.upProtocol(oldProtocol);
        thisProtocol.downProtocol(overThisProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol replaceProtocolOver(Protocol thisProtocol, Protocol overThisProtocol) {
        Protocol oldProtocol = overThisProtocol.upProtocol();
        overThisProtocol.upProtocol(thisProtocol);
        thisProtocol.upProtocol(oldProtocol.upProtocol());
        thisProtocol.downProtocol(overThisProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol addProtocolBelow(Protocol thisProtocol, Protocol belowThisProtocol) {
        Protocol oldProtocol = belowThisProtocol.downProtocol();
        oldProtocol.upProtocol(thisProtocol);
        belowThisProtocol.downProtocol(thisProtocol);
        thisProtocol.downProtocol(oldProtocol);
        thisProtocol.upProtocol(belowThisProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol replaceProtocolBelow(Protocol thisProtocol, Protocol belowThisProtocol) {
        Protocol oldProtocol = belowThisProtocol.downProtocol();
        belowThisProtocol.downProtocol(thisProtocol);
        thisProtocol.downProtocol(oldProtocol.downProtocol());
        thisProtocol.upProtocol(belowThisProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol replaceProtocol(Protocol protocol, int index) {
        if(index == 0)
            return replaceBottomProtocol(protocol);
        if(index == protocols.size() - 1)
            return replaceTopProtocol(protocol);
        Protocol oldProtocol = protocols.get(index);
        Protocol downProtocol = oldProtocol.downProtocol();
        Protocol upProtocol = oldProtocol.upProtocol();
        downProtocol.upProtocol(protocol);
        protocol.downProtocol(downProtocol);
        protocol.upProtocol(upProtocol);
        upProtocol.downProtocol(protocol);
        return protocols.set(index, protocol);
    }

    @Override
    public Protocol addTopProtocol(Protocol protocol) {
        Protocol oldProtocol = topProtocol();
        protocols.add(protocol);
        oldProtocol.upProtocol(protocol);
        protocol.downProtocol(oldProtocol);
        protocol.upProtocol(dummyEdgeProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol replaceTopProtocol(Protocol protocol) {
        Protocol oldProtocol = topProtocol();
        oldProtocol.downProtocol().upProtocol(protocol);
        protocol.downProtocol(oldProtocol.downProtocol());
        protocol.upProtocol(dummyEdgeProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol addBottomProtocol(Protocol protocol) {
        Protocol oldProtocol = bottomProtocol();
        protocols.add(0, protocol);
        oldProtocol.downProtocol(protocol);
        protocol.upProtocol(oldProtocol);
        protocol.downProtocol(dummyEdgeProtocol);
        return oldProtocol;
    }

    @Override
    public Protocol replaceBottomProtocol(Protocol protocol) {
        Protocol oldProtocol = bottomProtocol();
        oldProtocol.upProtocol().downProtocol(protocol);
        protocol.upProtocol(oldProtocol.upProtocol());
        protocol.downProtocol(dummyEdgeProtocol);
        return oldProtocol;
    }

    public static final class EdgeDummyProtocol extends AbstractProtocol {

        @Override
        public boolean checkEvent(Event event) {
            return false;
        }
    }
}
