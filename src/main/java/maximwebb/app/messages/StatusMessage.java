package maximwebb.app.messages;

import java.util.UUID;

/* Used for invoking an action on a node */
public class StatusMessage implements IMessage {

    private String message;
    private UUID authorId;
    private UUID recipientId;

    public StatusMessage(String message, UUID authorId) {
        this.message = message;
        this.authorId = authorId;
        this.recipientId = null;
    }

    public StatusMessage(String message, UUID authorId, UUID recipientId) {
        this.message = message;
        this.authorId = authorId;
        this.recipientId = recipientId;
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
