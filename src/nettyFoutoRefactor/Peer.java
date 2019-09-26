package nettyFoutoRefactor;

import nettyFoutoRefactor.network.*;
import nettyFoutoRefactor.network.messaging.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Peer implements IMessageConsumer, INodeListener {

    private static final String LISTEN_BASE_PORT = "listen_base_port";

    private static final Logger logger = LogManager.getLogger(PeerOutConnection.class);

    public Peer(int listenBasePort) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("network.properties"));
        } catch(IOException fnf) {
            logger.error("network.properties file not found. Aborting");
            System.exit(1);
        }

        if(listenBasePort <= 1023 || listenBasePort > 65535) {
            logger.error("Invalid port number, please insert a value between 1024 and 65535.");
            System.exit(1);
        }
        props.setProperty(LISTEN_BASE_PORT, Integer.toString(listenBasePort));

        try {
            INetwork network = new NetworkService(props);
            network.registerConsumer((byte) 10, this);
        } catch(Exception e) {
            logger.error("Error while instantiating the network service, please check.");
            System.exit(1);
        }
    }

    @Override
    public void deliverMessage(byte msgCode, Object msg, Host from) {

    }

    @Override
    public void nodeDown(Host peer) {

    }

    @Override
    public void nodeUp(Host peer) {

    }

    @Override
    public void nodeConnectionReestablished(Host peerHost) {

    }
}
