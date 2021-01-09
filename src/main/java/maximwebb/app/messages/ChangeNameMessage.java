package maximwebb.app.messages;

import java.util.UUID;

public class ChangeNameMessage implements IMessage {
    public final String name;
    public final UUID authorId;

    public ChangeNameMessage(String name, UUID authorId) {
        this.name = name;
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
