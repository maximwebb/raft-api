package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class Node {
    public UUID uid;
    public UUID networkId;
    private String name;
    private Link link;
    public NodeRole role;
    private ArrayList<IMessage> log = null;

    public Node(String name, NodeRole role) {
        this.name = name;
        this.role = role;
        uid = UUID.randomUUID();
    }

    public void connect(InetAddress address, int port) {
        link = new Link(this, address, port, role);
    }

    public void connect() {
        if (role != NodeRole.LEADER) {
            System.out.println("Error: Please specify IP address and port");
        }
        else {
            link = new Link(this);
        }
    }

    public void disconnect() {
        link.shutdown();
    }

    public void broadcast(String msg) {
        TextMessage textMessage = new TextMessage(msg, name, uid, null);
        link.broadcast(textMessage);
    }

    public void broadcast(IMessage msg) {

    }

    void deliver(IMessage message) {
        if (message instanceof TextMessage) {
            System.out.println(message.toString());
        }
    }

    public void promote() {
        role = NodeRole.LEADER;
        link.promote();
    }

    public void demote() {
        role = NodeRole.FOLLOWER;
        link.demote();
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(link.getAddress(), link.getPort(), networkId, uid, role);
    }
}
