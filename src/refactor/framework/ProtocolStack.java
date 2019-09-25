package refactor.framework;

public interface ProtocolStack {

    Protocol topProtocol();

    Protocol bottomProtocol();

    Protocol protocol(int index);

    Protocol addProtocol(Protocol protocol, int index);

    Protocol addProtocolOver(Protocol thisProtocol, Protocol overThisProtocol);

    Protocol replaceProtocolOver(Protocol thisProtocol, Protocol overThisProtocol);

    Protocol addProtocolBelow(Protocol thisProtocol, Protocol belowThisProtocol);

    Protocol replaceProtocolBelow(Protocol thisProtocol, Protocol belowThisProtocol);

    Protocol replaceProtocol(Protocol protocol, int index);

    Protocol addTopProtocol(Protocol protocol);

    Protocol replaceTopProtocol(Protocol protocol);

    Protocol addBottomProtocol(Protocol protocol);

    Protocol replaceBottomProtocol(Protocol protocol);
}
