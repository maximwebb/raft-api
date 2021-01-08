package maximwebb.app.messages;

import java.util.UUID;

/* Broadcast to receive node info from all nodes, send direct to node to request node info from single node. */
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
