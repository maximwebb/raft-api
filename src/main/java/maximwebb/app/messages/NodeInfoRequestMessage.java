package maximwebb.app.messages;

import java.util.UUID;

public class NodeInfoRequestMessage implements IMessage {

    public UUID authorId;

    public NodeInfoRequestMessage(UUID authorId) {
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
