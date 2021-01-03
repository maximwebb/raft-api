package maximwebb.app;

import maximwebb.app.client.Node;
import maximwebb.app.client.NodeInfo;
import maximwebb.app.client.NodeRole;

public class App {
    public static void main(String[] args) {
        Node node1 = new Node("Node 1", NodeRole.LEADER);
        Node node2 = new Node("Node 2", NodeRole.FOLLOWER);
        node1.connect();
        NodeInfo info = node1.getNodeInfo();
        node2.connect(info.address, info.port);
        node1.broadcast("Hi, I'm node 1!");
        node2.broadcast("Hi, I'm node 2!");
        Thread t = new Thread(() -> {
            while (true) {
                System.out.println(".");
            }
        });
    }
}
