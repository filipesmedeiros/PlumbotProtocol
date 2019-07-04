package refactor.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BBInetSocketAddress {
	
	public static final String MALFORMED_BUFFER = "The ByteBuffer doesn't contain correct information about"
			+ "a TCP IP address";
	
	public static final InetSocketAddress fromByteBuffer(ByteBuffer buffer)
			throws UnknownHostException {
		if(buffer.capacity() != 6)
			throw new IllegalArgumentException(MALFORMED_BUFFER);
		byte[] allBytes = buffer.array();
		byte[] addressBytes = Arrays.copyOfRange(allBytes, 0, 4);
		InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
		return new InetSocketAddress(inetAddress, buffer.getInt(4));
	}
}
