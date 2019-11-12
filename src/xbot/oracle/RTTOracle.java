package xbot.oracle;

import messages.Message;
import messages.xbot.oracle.PingMessage;
import network.Network;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class RTTOracle implements Oracle {

    public Map<InetSocketAddress, Pair<CompletableFuture<Long>, Long>> pings;

    private Network network;
    private InetSocketAddress id;

    private ExecutorService executor = Executors.newCachedThreadPool();

    public RTTOracle(Network network, InetSocketAddress id) {
        pings = new HashMap<>();

        this.network = network;
        this.id = id;
    }

    @Override
    public CompletableFuture<Long> getCost(InetSocketAddress peer) {
        network.send(new PingMessage(id).serialize(), peer);
        CompletableFuture<Long> future = executor.submit();

        pings.put(peer, new Pair<>(future, System.currentTimeMillis()));
        return future;
    }

    @Override
    public void deliverMessage(Message m) {
        if(m.type() != Message.MessageType.Ping || !(m instanceof PingMessage)) // sanity check
            return;
        Pair<CompletableFuture<Long>, Long> pingSent = pings.get(m.sender());
        if(pingSent == null)
            return;
        pingSent.u().complete(System.currentTimeMillis() - pingSent.v());
    }

    private static class Pair<U, V> {
        private U u;
        private V v;

        public Pair(U u, V v) {
            this.u = u;
            this.v = v;
        }

        public U u() {
            return u;
        }

        public V v() {
            return v;
        }
    }
}
