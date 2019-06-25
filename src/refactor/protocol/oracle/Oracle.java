package refactor.protocol.oracle;

import refactor.protocol.MessageListener;
import refactor.protocol.Node;

public interface Oracle extends MessageListener {

    long getCost(Node node);


}
