package nettyFoutoRefactor.network.messaging;

import refactor.GlobalSettings;
import refactor.exception.MessageTooLargeException;
import refactor.utils.BBInetSocketAddress;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * This class represents a {@link Message} that is sent between nodes of the network utilizing this protocol.
 * It has metadata and can have optional data, for when it is needed. Messages with no data are assumed to be
 * protocol control messages
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 20.06.2019
 */
public class Message {
	
	public final static String SENDER_LABEL = "snd";

    /**
     * Helper exception message for when a metadata entry's provided label is empty or {@code null}
     */
    public static final String NULL_ARGUMENT_LABEL = "Null or empty label not allowed";

    /**
     * Helper exception message for when a metadata entry's provided label is
     */
    public static final String BAD_ARGUMENT_LABEL_LENGTH = "Label length can't be greater than " + GlobalSettings.MAX_LABEL_SIZE;

    /**
     * Helper exception message for when a metadata entry's provided value is empty or {@code null}
     */
    public static final String NULL_ARGUMENT_DATA = "Null data not allowed";

    /**
     * Helper exception message for when the provided destination is empty, is {@code null} or is unresolved
     */
    private static final String BAD_ARGUMENT_DESTINATION = "Null destination not allowed";

    /**
     * This field stores all metadata associated with a message, either it is specific of general.
     * This object should provide a fast way to get a specific field of metadata, and also an efficient
     * way to encode the metadata into a {@link ByteBuffer}
     */
    private MessageMetadata metadata;

    /**
     * If the message has any data (client application data), it will be stored here
     */
    private ByteBuffer data;

    /**
     * This field is used within the program for the various layers of the protocol to communicate between each other.
     * It is not sent in the encoded message
     */
    private InetSocketAddress destination;

    /**
     * Default constructor for the {@link Message} class. Constructs a {@link Message} with the
     * given type, and no data or any other metadata
     * @param messageType This determines the type of this message (prune, join, etc.)
     */
    public Message(MessageSerializer.MessageType messageType) {
        metadata = new MessageMetadata(messageType);
        data = null;
    }

    /**
     * Convenience method to easily add a Peer to this message
     * @return This message
     */
    public Message withSender() {
        addMetadataEntry(SENDER_LABEL, GlobalSettings.localAddressBytes());
        return this;
    }

    /**
     * Convenience method to easily get the Peer of the message. This method assumes the metadata entry "snd" is there
     * @return The Peer of this message
     */
    public InetSocketAddress sender() {
        try {
            return BBInetSocketAddress.fromByteBuffer(metadataEntry(SENDER_LABEL));
        } catch(UnknownHostException uhe) {
            // TODO
            System.exit(1);
            return null;
        }
    }

    /**
     * Size of this {@link Message} in bytes. Includes: type, number of metadata entries for decoding,
     * the metadata entries, and the data present (if any), plus its size in bytes
     * @return The size of this {@link Message} in bytes
     */
    public int sizeInBytes() {
        return Byte.BYTES + Short.BYTES +
                metadata.sizeInBytes() + (data != null ? (Integer.BYTES + data.capacity()) : 0);
    }

    /**
     * Encodes the whole {@link Message} into a {@link ByteBuffer}, including metadata and data, if any.
     * Will be mostly used to send this {@link Message} to other nodes in the network
     * @return The encoded {@link ByteBuffer} with all this {@link Message}'s information
     * @throws MessageTooLargeException If the {@link Message}, in bytes, is larger than the defined limit
     * in {@link GlobalSettings}
     */
    public ByteBuffer encode()
            throws MessageTooLargeException {
        int sizeInBytes = sizeInBytes();
        if(sizeInBytes > GlobalSettings.MAX_MESSAGE_SIZE)
            throw new MessageTooLargeException(sizeInBytes);
        ByteBuffer messageBuffer = ByteBuffer.allocate(sizeInBytes);
        // Puts the type of the message in this byte
        messageBuffer.put(MessageSerializer.encodeMessageType(metadata.messageType()));
        // Puts the number of metadata entries this message has
        messageBuffer.putShort((short) metadata.metadata().size());

        // Encoding puts the size (in bytes) of both the label and the value
        // And then the bytes themselves
        metadata.metadata().forEach((label, value) -> {
            messageBuffer.put((byte) label.capacity());
            messageBuffer.putShort((short) value.capacity());
            messageBuffer.put(label);
            messageBuffer.put(value);
        });

        // Can't be null (didn't get set) and sanity check on its length
        if(data != null && data.capacity() != 0) {
            messageBuffer.putInt(data.capacity());
            messageBuffer.put(data);
        }

        // The Buffer has to be flipped to be read correctly elsewhere
        return (ByteBuffer) messageBuffer.flip();
    }

    /**
     * Simply returns this {@link Message}'s type. The possible types are defined in {@link MessageSerializer}
     * @return This {@link Message}'s type
     */
    public MessageSerializer.MessageType messageType() {
        return metadata.messageType();
    }

    /**
     * Simply returns this {@link Message}'s metadata object. It contains all entries currently
     * defined, plus some more info and any alterations in this map will also be made in the original one,
     * so use with caution
     * @return A reference to this {@link Message}'s metadata
     */
    public MessageMetadata metadata() {
        return metadata;
    }

    /**
     * Gets a specific entry of the metadata of this {@link Message}, in the form of a ByteBuffer, given its label,
     * or {@code null} if an entry with the given label is not found
     * @param label The label of the entry to get
     * @return The value of the requested metadata entry
     * @throws IllegalArgumentException If the label passed is either {@code null} or empty
     */
    public ByteBuffer metadataEntry(String label)
            throws IllegalArgumentException {
        if(label == null || label.length() == 0)
            throw new IllegalArgumentException(NULL_ARGUMENT_LABEL);
        return metadata.metadataEntry(label);
    }

    /**
     * Adds a new metadata entry to this {@link Message}'s metadata, with a given label and value
     * @param label The label to give to the metadata entry
     * @param value The value of the new metadata entry
     * @throws IllegalArgumentException If either the label or the data don't meet the standard requirements
     * (which means one of them is {@code null} or empty of the label's length is incorrect
     * @return This {@link Message} for convenience purposes and chaining
     */
    public Message addMetadataEntry(String label, ByteBuffer value)
            throws IllegalArgumentException {
        if(label == null || label.length() == 0)
            throw new IllegalArgumentException(NULL_ARGUMENT_LABEL);
        if(value == null || value.capacity() == 0)
            throw new IllegalArgumentException(NULL_ARGUMENT_DATA);

        // This method throws the exception when the label's length is incorrect
        metadata.addMetadata(label, value);
        return this;
    }

    /**
     * Set this message's data, in the form of a {@link ByteBuffer}.
     * @param data The data to store in this message
     * @return This message, for convenience purposes
     * @throws IllegalArgumentException If the passed {@link ByteBuffer} is null
     * or has no data in it ({@code {@link Buffer#capacity()} == 0})
     */
    public Message setData(ByteBuffer data) {
        if(data == null || data.capacity() == 0)
            throw new IllegalArgumentException(NULL_ARGUMENT_DATA);
        this.data = data;
        return this;
    }

    /**
     * This method returns the data stored in the message. If the message doesn't contain any data
     * (for example, a control message), this method returns {@code null}
     * @return The {@link ByteBuffer} containing all the message data, or {@code null} if no data exists
     */
    public ByteBuffer data() {
        return data;
    }

    /**
     * A simple setter method for this {@link Message}'s destination, that returns this object, for
     * convenience purposes
     * @param destination The destination of this {@link Message} (where it will be sent to)
     * @return This {@link Message}, for convenience purposes
     * @throws IllegalArgumentException Thrown if the passed {@link InetSocketAddress} is either {@code null}
     * or unresolved (given by {@link InetSocketAddress}'s {@code isUnresolved()}
     */
    public Message setDestination(InetSocketAddress destination)
            throws IllegalArgumentException {
        if(destination == null || destination.isUnresolved())
            throw new IllegalArgumentException(BAD_ARGUMENT_DESTINATION);
        this.destination = destination;
        return this;
    }

    /**
     * Simple getter method to have access to this {@link Message}'s destination
     * @return This {@link Message}'s destination, in the form of an {@link InetSocketAddress}
     */
    public InetSocketAddress getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Message))
            return false;
        Message otherMessage = (Message) other;
        if(!metadata.equals(otherMessage.metadata))
            return false;
        if(!destination.equals(otherMessage.destination))
            return false;
        return data.equals(otherMessage.data);
    }
}
