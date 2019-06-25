package test;

import message.plumtree.BodyMessage;
import protocol.PlumBot;
import protocol.PlumBotInstance;

import java.io.BufferedInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Consumer;

public class BasicApp implements Application {

    private void init() {
        List<InetSocketAddress> addressList = new LinkedList<>();
        List<PlumBot> instances = new LinkedList<>();

        String addr;
        try {
            addr = InetAddress.getByName(InetAddress.getLocalHost().getHostName()).getHostAddress();
        } catch(UnknownHostException e) {
            // TODO
            e.printStackTrace();
            return;
        }

        InetSocketAddress node = new InetSocketAddress(addr, 3000);
        addressList.add(node);
        System.out.println("Hosted at " + node.toString());

        PlumBot pb = new PlumBotInstance(node);
        pb.addApp(this);
        instances.add(pb);

        Consumer<InetSocketAddress> addNode = nodeToAdd -> {
            InetSocketAddress contact = addressList.get(new Random().nextInt(addressList.size()));
            addressList.add(nodeToAdd);
            System.out.println("Hosted at" + nodeToAdd);

            PlumBot newPb = new PlumBotInstance(nodeToAdd);
            instances.add(newPb);

            System.out.println("Joining at " + contact);
            newPb.join(contact);
        };

        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                InetSocketAddress node = new InetSocketAddress(addr, new Random().nextInt(5000) + 2000);
                addNode.accept(node);
            }
        };

        t.schedule(task, 3000, 3000);

        Scanner in = new Scanner(new BufferedInputStream(System.in));

        for(;;) {
            String input = in.nextLine();

            if(input.equals("stop"))
                t.cancel();
            else if(input.equals("list"))
                for(PlumBot instance : instances) {
                    System.out.println("-- Node: " + instance.id() + " --");

                    System.out.println("  --- Active view ---");
                    for(InetSocketAddress peer : instance.peerActiveView())
                        System.out.println("   - " + peer);

                    System.out.println("  --- Passive view ---");
                    for(InetSocketAddress peer : instance.passiveView())
                        System.out.println("   - " + peer);

                    System.out.println("------ x ------");
                }
            else if(input.equals("step")) {
                node = new InetSocketAddress(addr, new Random().nextInt(5000) + 2000);
                addNode.accept(node);
            } else if(input.equals("start")) {
                t = new Timer();

                /*TimerTask task1 = new TimerTask() {
                    @Override
                    public void run() {
                        InetSocketAddress local = new InetSocketAddress(addr, new Random().nextInt(5000) + 2000);

                        InetSocketAddress contact = addressList.get(new Random().nextInt(addressList.size()));
                        addressList.add(local);
                        System.out.println("Hosted at" + local);

                        PlumBot newPb = new PlumBotInstance(local);
                        instances.add(newPb);

                        System.out.println("Joining at " + contact);
                        newPb.join(contact);
                    }
                };*/

                t.schedule(task, 0, 3000);
            } else if(input.equals("quit")) {
                in.close();
                System.exit(0);
            }
        }
    }

    @Override
    public void deliver(BodyMessage message) {
        System.out.println("Received message");
    }

    public static void main(String[] args) {
        new BasicApp().init();
    }
}
