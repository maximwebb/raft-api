package maximwebb.app.messages;

import java.util.UUID;

/* Used for invoking an action on a node */
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
