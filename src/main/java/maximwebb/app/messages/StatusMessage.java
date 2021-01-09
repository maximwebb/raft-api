package maximwebb.app.messages;

import java.util.UUID;

/* Used as a heartbeat message */
public class StatusMessage implements IMessage {

    private final UUID authorId;

    public StatusMessage(UUID authorId) {
        this.authorId = authorId;
    }

    @Override
    public UUID getAuthorId() {
        return authorId;
    }

    @Override
    public UUID getRecipientId() {
        return null;
    }
}
