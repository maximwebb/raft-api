package maximwebb.app.messages;

import java.io.Serializable;
import java.util.UUID;

public interface IMessage extends Serializable {
    public UUID getAuthorId();
    public UUID getRecipientId();
}
