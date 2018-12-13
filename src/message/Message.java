package message;

import network.NetworkInterface;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Message {

    // End of string (mostly for node address)
    char EOS = '<';

    byte EOT = NetworkInterface.EOT;

    // Size (in bytes) of the identifier of message types
    int MSG_TYPE_SIZE = 2;

    // Maximum size of a message (in bytes)
    int MSG_SIZE = 2 * 1024 * 1024;

    ByteBuffer bytes();

    InetSocketAddress sender();

    int size();

    short messageType();
}
