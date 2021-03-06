package maximwebb.app.messages;

import maximwebb.app.client.NodeInfo;

import java.util.UUID;

public class NodeInfoMessage implements IMessage {

    public NodeInfo nodeInfo;
    public UUID recipientId;

    public NodeInfoMessage(NodeInfo nodeInfo, UUID recipientId) {
        this.nodeInfo = nodeInfo;
        this.recipientId = recipientId;
    }

    @Override
    public UUID getAuthorId() {
        return nodeInfo.nodeId;
    }

    @Override
    public UUID getRecipientId() {
        return recipientId;
    }
}
