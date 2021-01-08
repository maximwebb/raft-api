package maximwebb.app.ui;

import maximwebb.app.client.Node;
import maximwebb.app.client.NodeInfo;
import maximwebb.app.messages.TextMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class Console implements Runnable {
    private Scanner scanner;
    private Node node;
    private boolean socketOpen;

    public Console(String name) {
        scanner = new Scanner(System.in);
        node = new Node(name);
        socketOpen = false;
        run();
    }

    @Override
    public void run() {
        String input;
        while (!((input = scanner.nextLine()) == null)) {
            if (input.charAt(0) == '/' && input.length() > 1) {
                String[] args = input.substring(1).split(" ");
                String command = args[0];
                switch (command) {
                    case "create":
                    case "createNetwork":
                        int port = args.length > 1 ? Integer.parseInt(args[1]) : 0;
                        node.createNetwork(port);
                        socketOpen = true;
                        System.out.println("Created network on address " + node.serverSocket.getLocalSocketAddress() + ", port: " + node.serverSocket.getLocalPort());
                        break;
                    case "connect":
                        if (args.length == 3 && StringUtils.isNumeric(args[2])) {
                            try {
                                node.connect(InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
                                socketOpen = true;
                            } catch (IOException e) {
                                System.out.println("Could not connect to " + args[1]);
                            }
                        } else {
                            System.out.println("Usage: /connect InetAddress port");
                        }
                        break;
                    case "disconnect":
                        node.disconnect();
                        socketOpen = false;
                        break;
                    case "dm":
                        if (args.length == 3) {
                            UUID uuid;
                            try {
                                uuid = UUID.fromString(args[1]);
                            } catch (IllegalArgumentException e) {
                                uuid = null;
                            }
                            if (uuid != null && node.hasNodeId(uuid)) {
                                TextMessage msg = new TextMessage(args[2], node.name, node.uid, UUID.fromString(args[1]));
                                node.sendDirectMessage(msg);
                                String recipient = node.getNodeById(UUID.fromString(args[1])).name;
                                System.out.println(msg.getTimeStamp() + " [" + node.name + "] --> " + recipient + ": " + args[2]);
                            } else {
                                Set<NodeInfo> nodes = node.getNodesByName(args[1]);
                                if (nodes.size() == 0) {
                                    System.out.println("Could not find node with name: \"" + args[1] + "\".");
                                } else if (nodes.size() == 1) {
                                    NodeInfo recipient = nodes.iterator().next();
                                    TextMessage msg = new TextMessage(args[2], node.name, node.uid, recipient.nodeId);
                                    node.sendDirectMessage(msg);
                                    System.out.println(msg.getTimeStamp() + " [" + node.name + "]-->[" + args[1] + "]: " + args[2]);
                                } else {
                                    System.out.println("Duplicate name - please specify direct message by UUID:");
                                    for (NodeInfo info : nodes) {
                                        System.out.println("UUID: " + info.nodeId);
                                    }
                                }
                            }
                        } else {
                            System.out.println("Usage: /dm name|UUID message");
                        }
                        break;
                    default:
                        System.out.println("Invalid command");
                        break;
                }
            } else if (input.equals("/")) {
                System.out.println("Invalid command");
            } else {
                if (socketOpen) {
                    node.broadcast(input);
                } else {
                    System.out.println("Create or join a network to send messages.");
                }
            }
        }
    }
}