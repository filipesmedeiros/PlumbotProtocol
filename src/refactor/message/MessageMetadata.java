package refactor.message;

import refactor.GlobalSettings;
import refactor.exception.InvalidMessageTypeException;
import refactor.utils.Constants;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// TODO Javadoc this class
/**
 * This class stores information about a {@link Message}'s metadata. It has all the entries
 * that have been defined, and is also responsible for encoding said metadata.
 * The metadata is composed of entries, with each one having a {@code label}, that is its
 * identifying name, and a {@code value} which is, obviously, its value
 * @author Filipe Medeiros - filipesilvamedeiros@gmail.com
 * @author Filipe Medeiros - github.com/filipesmedeiros
 * @version 1.0
 * @since 20.06.2019
 */
public class MessageMetadata {

    private MessageDecoder.MessageType messageType;
    private Map<ByteBuffer, ByteBuffer> metadata;
    private int sizeInBytes;

    MessageMetadata(MessageDecoder.MessageType messageType) {
        this.messageType = messageType;
        metadata = new HashMap<>();
        sizeInBytes = 0;
    }

    public MessageDecoder.MessageType messageType() {
        return messageType;
    }

    public void addMetadata(String label, ByteBuffer value)
            throws IllegalArgumentException {
        if(label == null)
            throw new IllegalArgumentException(Message.NULL_ARGUMENT_LABEL);
        if(value == null)
            throw new IllegalArgumentException(Message.NULL_ARGUMENT_DATA);

        ByteBuffer labelAsBytes = GlobalSettings.CHARSET.encode(label);

        if(labelAsBytes.capacity() > GlobalSettings.MAX_LABEL_SIZE)
            throw new IllegalArgumentException(Message.BAD_ARGUMENT_LABEL_LENGTH);

        metadata.put(labelAsBytes, value);
        sizeInBytes += Constants.SIZE_OF_BYTE + Constants.SIZE_OF_SHORT + labelAsBytes.capacity() + value.capacity();
    }

    public int sizeInBytes() {
        return sizeInBytes;
    }

    public Map<ByteBuffer, ByteBuffer> metadata() {
        return metadata;
    }

    public ByteBuffer metadataEntry(String label)
            throws IllegalArgumentException {
        if(label == null)
            throw new IllegalArgumentException(Message.NULL_ARGUMENT_LABEL);

        return metadata.get(GlobalSettings.CHARSET.encode(label));
    }

    /**
     * Override of the {@link Object}'s {@code equals(Object other)} method. Simply compares all metadata entries.
     * All of them have to be equal for the {@link MessageMetadata}s to be equal
     * @param other The other {@link Object} (most likely an instance of {@link MessageMetadata}) to compare with
     * {@code this}
     * @return True if both {@link MessageMetadata}s are equal (all metadata entries are equal)
     */
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof MessageMetadata))
            return false;
        else {
            MessageMetadata otherMetadata = (MessageMetadata) other;
            if(messageType != otherMetadata.messageType)
                return false;
            if(sizeInBytes != otherMetadata.sizeInBytes)
                return false;

            for(Map.Entry<ByteBuffer, ByteBuffer> entry : metadata.entrySet()) {
                ByteBuffer otherValue = otherMetadata.metadata.get(entry.getKey());
                if(otherValue == null)
                    return false;
                if(!otherValue.equals(entry.getValue()))
                    return false;
            }
        }

        return true;
    }
}
