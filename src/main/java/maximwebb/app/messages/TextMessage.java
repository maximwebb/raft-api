package maximwebb.app.messages;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class TextMessage implements IMessage {
    public String message;
    public String author;
    public UUID authorId;
    public UUID recipientId;
    public Date timestamp;

    public TextMessage(String message, String author, UUID authorId, UUID recipientId) {
        this.message = message;
        this.author = author;
        this.authorId = authorId;
        this.recipientId = recipientId;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        if (recipientId == null) {
            return getTimeStamp() + " [" + author + "]: " + message;
        } else {
            return getTimeStamp() + " [" + author + "] (Direct): " + message;
        }
    }

    @Override
    public UUID getAuthorId() {
        return authorId;
    }

    @Override
    public UUID getRecipientId() {
        return recipientId;
    }

    public String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z");
        return sdf.format(timestamp);
    }
}
