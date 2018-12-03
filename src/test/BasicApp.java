package test;

import protocol.PlumBot;
import protocol.PlumBotInstance;

import java.io.BufferedInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

public class BasicApp {
    public static void main(String[] args) {
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
        
        InetSocketAddress local = new InetSocketAddress(addr, 3000);
        addressList.add(local);
        System.out.println("Hosted at " + local.toString());

        PlumBot pb = new PlumBotInstance(local);
        instances.add(pb);

        Timer t = new Timer();
        TimerTask task = new TimerTask() {
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
        };

        t.schedule(task, 0, 3000);

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
                local = new InetSocketAddress(addr, new Random().nextInt(5000) + 2000);

                InetSocketAddress contact = addressList.get(new Random().nextInt(addressList.size()));
                addressList.add(local);
                System.out.println("Hosted at" + local);

                PlumBot newPb = new PlumBotInstance(local);
                instances.add(newPb);

                System.out.println("Joining at " + contact);
                newPb.join(contact);
            } else if(input.equals("start")) {
                t = new Timer();

                TimerTask task1 = new TimerTask() {
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
                };

                t.schedule(task1, 0, 3000);
            } else if(input.equals("quit")) {
                in.close();
                System.exit(0);
            }
        }
    }
}
