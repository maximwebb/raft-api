package maximwebb.app.messages;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class TextMessage implements IMessage {
    public String message;
    public UUID uid;
    public String author;
    public Date timestamp;

    public TextMessage(String message, UUID uid, String author) {
        this.message = message;
        this.uid = uid;
        this.author = author;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z");
        return sdf.format(timestamp) + " [" + author + "]: " + message;
    }
}
