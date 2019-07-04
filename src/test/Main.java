package test;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramSocket;

public class Main {
	
	public static void main(String[] args) throws UnknownHostException, SocketException {

		try(final DatagramSocket socket = new DatagramSocket()) {
		  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
		  System.out.println(socket.getLocalAddress().getHostAddress());
		}
		
		System.out.print(InetAddress.getLocalHost());
	}

}
