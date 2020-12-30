package maximwebb.app;

import java.util.ArrayList;
import java.util.UUID;

public class Node implements INode {
    public UUID uid;
    private UUID networkId;
    private ArrayList<IMessage> log = null;

    public Node() {
        uid = UUID.randomUUID();
    }

    public void connect(UUID nId) {

    }

    public void disconnect() {

    }

    public void broadcast(IMessage msg) {

    }
}
