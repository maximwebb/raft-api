package maximwebb.app;

import maximwebb.app.client.Node;
import maximwebb.app.server.MessageServer;

import java.io.IOException;
import java.net.InetAddress;

public class App {
    public static void main(String[] args) {
        MessageServer server = new MessageServer(53229);
        Node node1 = new Node("Node 1");
        Node node2 = new Node("Node 2");
        try {
            node1.connect(InetAddress.getByName("127.0.1.1"), 53229);
            node2.connect(InetAddress.getByName("127.0.1.1"), 53229);
            node1.broadcast("Hi, I'm node 1!");
            node2.broadcast("Hi, I'm node 2!");
            Thread t = new Thread(() -> {
                while (true) {
                    System.out.println(".");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
