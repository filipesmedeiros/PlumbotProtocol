package refactor.utils;

import refactor.GlobalSettings;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class BBInetSocketAddress {

	/**
	 * This field is used for storing the size, in bytes, of an IP/Port address (4 for the address, 2 for the port)
	 */
	private static final byte IPPORTBUFFER_SIZE = 6;
	
	public static final String MALFORMED_BUFFER = "The ByteBuffer doesn't contain correct information about"
			+ "a TCP IP address";
	
	public static InetSocketAddress fromByteBuffer(ByteBuffer buffer)
			throws UnknownHostException {
		if(buffer.capacity() != 6)
			throw new IllegalArgumentException(MALFORMED_BUFFER);
		byte[] addressBytes = new byte[4];
		for(int i = 0; i < 4; i++)
			addressBytes[i] = buffer.get();
		InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
		return new InetSocketAddress(inetAddress, buffer.getShort());
	}

	public static ByteBuffer toByteBuffer(InetSocketAddress inetSocketAddress) {
		ByteBuffer inetAddressBuffer = ByteBuffer.allocate(IPPORTBUFFER_SIZE);
		inetAddressBuffer.put(inetSocketAddress.getAddress().getAddress());
		inetAddressBuffer.putShort((short) GlobalSettings.localAddress().getPort());
		return inetAddressBuffer;
	}

	public static ByteBuffer localToByteBuffer() {
		return toByteBuffer(GlobalSettings.localAddress());
	}
}
