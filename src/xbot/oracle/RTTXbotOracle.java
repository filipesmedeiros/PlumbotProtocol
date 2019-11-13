package xbot.oracle;

import babel.exceptions.HandlerRegistrationException;
import babel.protocol.GenericProtocol;
import babel.protocol.event.ProtocolMessage;
import babel.requestreply.ProtocolRequest;
import network.Host;
import network.INetwork;
import xbot.oracle.messages.PingMessage;
import xbot.oracle.messages.PingReplyMessage;
import xbot.oracle.notifications.CostNotification;
import xbot.oracle.requests.CostRequest;

import java.util.Map;
import java.util.Properties;

public class RTTXbotOracle extends GenericProtocol {

    public static final short PROTOCOL_CODE = 300;
    public static final String PROTOCOL_NAME = "RTT X-Bot Oracle";

    public Map<Host, Long> pings;

    public RTTXbotOracle(INetwork net) throws HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_CODE, net);

        registerRequestHandler(CostRequest.REQUEST_CODE, this::handleCostRequest);
    }

    @Override
    public void init(Properties properties) {
    }

    private void handleCostRequest(ProtocolRequest r) {
        if(!(r instanceof CostRequest))
            return;

        CostRequest req = (CostRequest) r;

        if(pings.containsKey(req.peer()))
            return;

        PingMessage message = new PingMessage();
        sendMessage(message, req.peer());
        pings.put(req.peer(), System.currentTimeMillis());
    }

    private void handlePing(ProtocolMessage m) {
        if(!(m instanceof PingMessage))
            return;

        PingMessage ping = (PingMessage) m;

        if(pings.containsKey(ping.getFrom())) {
            long cost = System.currentTimeMillis() - pings.get(ping.getFrom());
            PingReplyMessage pingReply = new PingReplyMessage().setCost(cost);
            sendMessage(pingReply, ping.getFrom());

            CostNotification costNotification = new CostNotification(cost);
            triggerNotification(costNotification);
        } else {
            PingMessage pingReply = new PingMessage();
            sendMessage(pingReply, ping.getFrom());
        }
    }

    private void handlePingWithCost(ProtocolMessage m) {
        if(!(m instanceof PingReplyMessage))
            return;

        if(pings.containsKey())
    }
}
