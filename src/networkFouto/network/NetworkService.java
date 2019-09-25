package networkFouto.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import networkFouto.network.messaging.NetworkMessage;
import networkFouto.network.messaging.control.ControlMessage;
import networkFouto.network.pipeline.InEventExceptionHandler;
import networkFouto.network.pipeline.InHandshakeHandler;
import networkFouto.network.pipeline.MessageDecoder;
import networkFouto.network.pipeline.MessageEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NetworkService implements INetwork {

    private static final Logger logger = LogManager.getLogger(NetworkService.class);

    public final static AttributeKey<Boolean> TRANSIENT_KEY = AttributeKey.valueOf("transient");

    private Bootstrap clientBootstrap;
    private Channel serverChannel;
    private final Host myHost;
    private Map<Host, PeerOutConnection> knownPeers = new ConcurrentHashMap<>();

    private Set<INodeListener> nodeListeners = ConcurrentHashMap.newKeySet();
    private Map<Byte, ISerializer> serializers = new ConcurrentHashMap<>();
    private Map<Byte, IMessageConsumer> messageConsumers = new ConcurrentHashMap<>();

    private NetworkConfiguration config;

    private int nSent;

    public NetworkService(Properties props) throws Exception {
        nSent = 0;
        config = new NetworkConfiguration(props);
        myHost = readHost();
        clientBootstrap = setupClientBootstrap();
        serverChannel = startServer(myHost.getPort());
        serializers.put(ControlMessage.MSG_CODE, ControlMessage.serializer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.debug("Killed")));

    }

    @Override
    public Host myHost() {
        return myHost;
    }

    @Override
    public void registerNodeListener(INodeListener listener) {
        nodeListeners.add(listener);
    }

    @Override
    public void registerConsumer(byte msgCode, IMessageConsumer consumer) {
        if (messageConsumers.putIfAbsent(msgCode, consumer) != null)
            throw new AssertionError("Trying to re-register consumer in NetworkService: " + msgCode);
    }

    @Override
    public void registerSerializer(byte msgCode, ISerializer serializer) {
        if (serializers.putIfAbsent(msgCode, serializer) != null)
            throw new AssertionError("Trying to re-register serializer in NetworkService" + msgCode);
    }

    @Override
    public void addPeer(Host peerHost) {
        knownPeers.computeIfAbsent(peerHost, k ->
                new PeerOutConnection(k, myHost, clientBootstrap, nodeListeners, serializers, config));
        //TODO return connection future/callback?
    }

    @Override
    public void removePeer(Host peerHost) {
        logger.info("Removing peer: " + peerHost);
        PeerOutConnection conn = knownPeers.get(peerHost);
        if (conn != null) {
            conn.terminate();
            knownPeers.remove(peerHost);
        }
        //TODO return connection future/callback?
    }

    @Override
    public boolean isConnectionActive(Host peerHost) {
        PeerOutConnection conn = knownPeers.get(peerHost);
        return conn != null && conn.getStatus() == PeerOutConnection.Status.ACTIVE;
    }

    @Override
    public void sendMessage(byte msgCode, Object payload, Host to, boolean newChannel) {

        logger.debug((newChannel ? "Transient " : " ") + "To " + to + ": " + payload.toString());

        if (to.equals(myHost)) {
            messageConsumers.get(msgCode).deliverMessage(msgCode, payload, myHost);
            return;
        }

        nSent++;
        PeerOutConnection connection = knownPeers.get(to);
        if (connection == null) {
            logger.error("Sending message to unknown peer... forgot to use addPeer? " + payload + " " + to);
            return;
        }

        NetworkMessage networkMessage = new NetworkMessage(msgCode, payload);
        if (newChannel)
            connection.sendMessageTransientChannel(networkMessage);
        else
            connection.sendMessage(networkMessage);
        //channelFuture.addListener(l -> logger.info("Message Sent")); TODO: return this channel future to caller
    }

    @Override
    public void sendMessage(byte msgCode, Object payload, Host to) {
        sendMessage(msgCode, payload, to, false);
    }

    @Override
    public void broadcastMessage(byte msgCode, Object payload, Iterator<Host> targets) {
        while (targets.hasNext()) {
            Host h = targets.next();
            sendMessage(msgCode, payload, h);
        }
        //TODO return something?
    }

    //One-Time bootstrap functions
    private Channel startServer(int port) throws Exception {
        //TODO change groups options
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                //Write Idle handler - to send heartbeats
                ch.pipeline().addLast("IdleStateHandler", new IdleStateHandler(0, config.HEARTBEAT_INTERVAL_MILLIS, 0,
                                                                               TimeUnit.MILLISECONDS));
                ch.pipeline().addLast("MessageDecoder", new MessageDecoder(serializers));
                ch.pipeline().addLast("MessageEncoder", new MessageEncoder(serializers));
                ch.pipeline().addLast("InHandshakeHandler", new InHandshakeHandler(messageConsumers));
                ch.pipeline().addLast("InEventExceptionHandler", new InEventExceptionHandler());
            }
        });
        //TODO: study options / child options
        b.option(ChannelOption.SO_BACKLOG, 128);
        b.childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture f = b.bind(port).sync();
        logger.debug("Server started in port " + port);

        f.channel().closeFuture().addListener(cf -> {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        });
        return f.channel();
    }

    private Bootstrap setupClientBootstrap() {
        //TODO change group options
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap newClientBootstrap = new Bootstrap();
        newClientBootstrap.group(workerGroup);
        newClientBootstrap.channel(NioSocketChannel.class);
        //TODO: study options
        newClientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        newClientBootstrap.option(ChannelOption.TCP_NODELAY, true);
        newClientBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.CONNECT_TIMEOUT_MILLIS);
        return newClientBootstrap;
    }

    private Host readHost() throws Exception {
        //read IP/port
        short myPort = config.LISTEN_BASE_PORT;
        InetAddress myIp;
        String ip = config.LISTEN_ADDRESS;
        if (ip != null && !ip.isEmpty()) {
            myIp = InetAddress.getByName(ip);
        } else {
            myIp = getNetworkInterfaceAddress(config.LISTEN_INTERFACE);
        }
        if (myIp == null)
            throw new Exception("Error getting local ip address");

        return new Host(myIp, myPort);
    }

    //From Apache Cassandra
    private static InetAddress getNetworkInterfaceAddress(String interfaceName) throws Exception {
        try {
            NetworkInterface ni = NetworkInterface.getByName(interfaceName);
            if (ni == null)
                throw new Exception("Interface " + interfaceName + " could not be found");
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            if (!addresses.hasMoreElements())
                throw new Exception("Interface " + interfaceName + " was found, but had no addresses");
            /*
             * Try to return the first address of the preferred type, otherwise return the first address
             */
            while (addresses.hasMoreElements()) {
                InetAddress temp = addresses.nextElement();
                if (temp instanceof Inet4Address) return temp;
            }
            return null;
        } catch (SocketException e) {
            throw new Exception("Interface " + interfaceName + " caused an exception", e);
        }
    }

    @Override
    public int getnSent() {
        return nSent;
    }
}
