package plumbot;

import common.BroadcastListener;
import plumtree.Plumtree;
import xbot.Oracle;
import xbot.XBot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Random;

public class Plumbot {

    private XBot xbot;
    private Plumtree plumtree;

    public Plumbot(InetSocketAddress id, BroadcastListener broadcastListener) {
        Oracle oracle = peer -> new Random().nextLong() / 1000000000L;

        Properties props = new Properties();
        try {
            props.load(new InputStreamReader(new FileInputStream("config.properties")));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading config file.");
            System.exit(1);
        }

        xbot = new XBot(id, oracle,
                Integer.parseInt(props.getProperty("xbot_fanout")),
                Integer.parseInt(props.getProperty("xbot_k")),
                Integer.parseInt(props.getProperty("xbot_unbiased_peers")),
                Integer.parseInt(props.getProperty("xbot_passive_scan_length")),
                Integer.parseInt(props.getProperty("xbot_active_random_walk_length")),
                Integer.parseInt(props.getProperty("xbot_passive_random_walk_length")),
                Integer.parseInt(props.getProperty("xbot_optimization_period")),
                Integer.parseInt(props.getProperty("xbot_threshold")));

        plumtree = new Plumtree(id,
                Integer.parseInt(props.getProperty("plumtree_threshold")),
                Integer.parseInt(props.getProperty("plumtree_first_timer")),
                Integer.parseInt(props.getProperty("plumtree_second_timer")),
                xbot);

        xbot.setTreeBroadcast(plumtree);
        plumtree.setBroadcastListener(broadcastListener);

    }

    public void join(InetSocketAddress connectPeer) {
        xbot.join(connectPeer);
    }
}
