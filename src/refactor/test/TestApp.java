package refactor.test;

import refactor.GlobalSettings;
import refactor.exception.IllegalSettingChangeException;
import refactor.exception.SingletonIsNullException;
import refactor.network.TCP;
import refactor.protocol.xbot.XBotNode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class TestApp {

    public static void main(String... args) {
        try {
            if(args.length >= 3)
                GlobalSettings.DEBUGGING_LEVEL = (byte) Integer.parseInt(args[2]);

            XBotNode.xBotNode();
            try {
                int localPort = Integer.parseInt(args[0]);
                GlobalSettings.setLocalAddress(new InetSocketAddress(InetAddress.getLocalHost(), localPort));
                if(GlobalSettings.DEBUGGING_LEVEL >= 4)
                    System.out.println("Server TCP address is: " + GlobalSettings.localAddress());
            } catch(IllegalSettingChangeException | UnknownHostException e) {
                e.printStackTrace();
                System.exit(1);
            }

            TCP.tcp();

            InetAddress address = null;
            try {
                address = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                // TODO
                System.exit(1);
            }
            if(address == null)
                return;

            System.out.println("Started protocol on address " + GlobalSettings.localAddress());

            int connectPort = Integer.parseInt(args[1]);
            if (connectPort != 0)
                TCP.tcp().connect(new InetSocketAddress(address, connectPort));
        } catch(IndexOutOfBoundsException iobe) {
            iobe.printStackTrace();
            System.exit(1);
        }
    }
}
