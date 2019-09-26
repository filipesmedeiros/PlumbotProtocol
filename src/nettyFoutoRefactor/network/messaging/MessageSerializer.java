package nettyFoutoRefactor.network.messaging;

import io.netty.buffer.ByteBuf;
import nettyFoutoRefactor.network.ISerializer;
import refactor.GlobalSettings;
import refactor.exception.InvalidMessageTypeException;
import refactor.exception.NullMessageException;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class is responsible for storing some important, static information about the possible {@link Message}
 * types, and also for decoding incoming {@link Message}s
 * with a type that is not correct (not present in {@link MessageSerializer}'s type list
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 21.06.2019
 */
public class MessageSerializer implements ISerializer<Message> {

    /**
     * This is the number of types of {@link Message}s that can be sent by nodes (and therefore,
     * that nodes recognize). This number is used to check if encoding was done correctly, because type codes go
     * from {@code 0} to this value. The list of these types is described in {@link MessageType}
     */
    public static final int NUMBER_MESSAGE_TYPES = 17;

    @Override
    public void serialize(Message message, ByteBuf out) {
        
    }

    @Override
    public Message deserialize(ByteBuf in) throws UnknownHostException {
        return null;
    }

    @Override
    public int serializedSize(Message message) {
        return 0;
    }

    /**
     * This enum contains all types of {@link Message}s, with their respective names. Each one has a
     * specific function in the network
     */
    public enum MessageType {
        content,
        graft,
        hop,
        iHave,
        prune,
        request,

        join,
        acceptJoin,
        disconnect,
        forwardJoin,
        optimization,
        optimizationReply,
        pingBack,
        ping,
        replace,
        replaceReply,
        swap; // This is the Switch message but switch is a reserved keyword in java...

        /**
         * For encoding purposes, the code of each type is equal to the its position's ordinal in this enum. This is an
         * easy way to obtain unique codes for every type. Note that the type is converted to a byte, to safe resources
         * of the network.
         * @return The code of the specified {@link Message} type, in the form of a byte
         */
        public byte code() {
            return (byte) ordinal();
        }
    }

    /**
     * This method only decodes the {@link Message} type. This can be useful to save time when the program
     * only needs to know the type of the message, and not its content
     * @param encodedMessage All the bytes of the {@link Message} that needs to be decoded
     * @return The corresponding enum value of the {@link Message}
     * @throws NullMessageException Thrown if the passed {@link Message} is either null of empty
     * @throws InvalidMessageTypeException Thrown if the code of the {@link Message} is not one of the
     * possible codes, stored in the enum {@link MessageType} stored in this class
     */
    public static MessageType decodeMessageType(ByteBuffer encodedMessage)
            throws NullMessageException, InvalidMessageTypeException {
        // Sanity check
        if(encodedMessage == null || encodedMessage.capacity() == 0)
            throw new NullMessageException();

        // The first byte is where the type code is stored
        byte messageType = encodedMessage.get(0);
        // We can just do this, because it's the ordinal of the enum
        if(messageType < 0 || messageType >= NUMBER_MESSAGE_TYPES)
            throw new InvalidMessageTypeException(messageType);

        return MessageType.values()[messageType];
    }

    /**
     * This method is used to decode a message that is encoded in a ByteBuffer into a {@link Message} object.
     * It is dependant on the way the message was encoded, and in the future, would like to make this encoding process's
     * settings into a readable object, as to make the program more consistent. Note that, while receiving, the first 4
     * bytes of the buffer are an {@code int} indicating the total size of the {@link Message}, however, this is not
     * accounted for here because it is assumed that those 4 bytes are not included in this passed ByteBuffer
     * @param encodedMessage The ByteBuffer containing all the encoded information about the {@link Message}
     * @return The {@link Message} object, with all the decoded information
     * @throws NullMessageException If the {@link Message} is either {@code null} or empty
     * @throws InvalidMessageTypeException If the type of the {@link Message} (the first byte) is not defined in
     * {@link MessageType}
     */
    public static Message decodeMessage(ByteBuffer encodedMessage)
            throws NullMessageException, InvalidMessageTypeException {
        // Creating the Message which will be return in the end
        Message message = new Message(decodeMessageType(encodedMessage));

        // Advancing the Buffer to the second byte (first is MessageType)
        encodedMessage.get();

        // This is the encoded number of metadata entries the Message has
        short numberOfMetadataEntries = encodedMessage.getShort();
        for(short mdEntry = 0; mdEntry < numberOfMetadataEntries; mdEntry++) {
            // The size, in bytes, of the label of the metadata entry, and allocating a ByteBuffer for it
            byte labelSize = encodedMessage.get();
            ByteBuffer labelBuffer = ByteBuffer.allocate(labelSize);
            // Same as before, for the value
            short valueSize = encodedMessage.getShort();
            ByteBuffer valueBuffer = ByteBuffer.allocate(valueSize);

            // Reading both the label and the value of the metadata entry
            for(byte b = 0; b < labelSize; b++)
                labelBuffer.put(encodedMessage.get());
            for(short b = 0; b < valueSize; b++)
                valueBuffer.put(encodedMessage.get());

            message.addMetadataEntry(GlobalSettings.CHARSET.decode(labelBuffer).toString(), valueBuffer);
        }

        // Some Messages have no data, so we can end here
        if(encodedMessage.hasRemaining())
            return message;
        // Or we continue reading if there is data to read

        // Get the size of the data, in bytes, and allocate a Buffer for it
        int dataSize = encodedMessage.getInt();
        ByteBuffer dataBuffer = ByteBuffer.allocate(dataSize);
        for(int b = 0; b < dataSize; b++)
            dataBuffer.put(encodedMessage.get());
        message.setData(dataBuffer);
        // When we get to this point, nothing else if left to read, but a sanity check can be made
        if(encodedMessage.remaining() != 0 && GlobalSettings.DEBUGGING_LEVEL >= 2)
            System.out.println("Some bytes were left to decode in the Message");

        return message;
    }

    /**
     * Simple method to encode the type of the {@link Message}. It just calls the method defined in
     * {@link MessageType} that defines de code of each type
     * @param messageType The {@link Message} type whom code is needed
     * @return The code of the type, in the form of a byte
     */
    public static byte encodeMessageType(MessageType messageType) {
        return messageType.code();
    }
}
