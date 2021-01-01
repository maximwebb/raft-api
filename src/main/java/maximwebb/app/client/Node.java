package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

public class Node implements INode {
    public UUID uid;
    private UUID networkId;
    private String name;
    private Link link;
    private ArrayList<IMessage> log = null;

    public Node(String name) {
        this.name = name;
        uid = UUID.randomUUID();
    }

    public void connect(InetAddress address, int port) {
        link = new Link(this, address, port);
    }

    public void disconnect() {
        link.shutdown();
    }

    public void broadcast(String msg) {
        TextMessage textMessage = new TextMessage(msg, uid, name);
        link.broadcast(textMessage);
    }

    public void broadcast(IMessage msg) {

    }

    void deliver(IMessage message) {
        if (message instanceof TextMessage) {
            System.out.println(message.toString());
        }
    }
}
