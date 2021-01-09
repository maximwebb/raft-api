package maximwebb.app.ui;

import maximwebb.app.client.Node;
import maximwebb.app.client.NodeInfo;
import maximwebb.app.messages.ChangeNameMessage;
import maximwebb.app.messages.TextMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Console implements Runnable {
    private Scanner scanner;
    private Node node;
    private boolean socketOpen;
    private boolean DEBUGGING_MODE = false;

    public Console(String name) {
        scanner = new Scanner(System.in);
        node = new Node(name);
        socketOpen = false;
        System.out.println("                         ||==========================||");
        System.out.println("=========================|| MESSAGE SHARING PROTOCOL ||=========================");
        System.out.println("                         ||==========================||");
        System.out.println();
        run();
    }

    @Override
    public void run() {
        if (DEBUGGING_MODE) {
            int port = 30000;
            if (node.name.equals("User")) {
                node.createNetwork(port);
                socketOpen = true;
                System.out.println("Created network on address " + node.serverSocket.getLocalSocketAddress() + ", port: " + node.serverSocket.getLocalPort());
            } else {
                try {
                    node.connect(InetAddress.getByName("127.0.0.1"), port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socketOpen = true;
            }
        }
        String input;
        while (!((input = scanner.nextLine()) == null)) {
            if (input.length() > 0 && input.charAt(0) == '/' && input.length() > 1) {
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
                        if (args.length >= 3) {
                            UUID uuid;
                            try {
                                uuid = UUID.fromString(args[1]);
                            } catch (IllegalArgumentException e) {
                                uuid = null;
                            }
                            String message = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                            if (uuid != null && node.hasNodeId(uuid)) {
                                TextMessage msg = new TextMessage(message, node.getName(), node.uid, UUID.fromString(args[1]));
                                node.sendDirectMessage(msg);
                                String recipient = node.getNodeById(UUID.fromString(args[1])).name;
                                System.out.println(msg.getTimeStamp() + " [" + node.getName() + "] --> " + recipient + ": " + args[2]);
                            } else {
                                Set<NodeInfo> nodes = node.getNodesByName(args[1]);
                                if (nodes.size() == 0) {
                                    System.out.println("Could not find node with name: \"" + args[1] + "\".");
                                } else if (nodes.size() == 1) {
                                    NodeInfo recipient = nodes.iterator().next();
                                    TextMessage msg = new TextMessage(message, node.getName(), node.uid, recipient.nodeId);
                                    node.sendDirectMessage(msg);
                                    System.out.println(msg.getTimeStamp() + " [" + node.getName() + "]-->[" + args[1] + "]: " + message);
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
                    case "nick":
                    case "name":
                    case "setname":
                        if (args.length >= 2) {
                            String name = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                            ChangeNameMessage nameMessage = new ChangeNameMessage(name, node.uid);
                            node.broadcast(nameMessage);
                        } else {
                            System.out.println("Usage: /" + command + " nickname");
                        }
                        break;
                    /* For debugging purposes only. */
                    case "stopheartbeat":
                        node.heartbeatHandler.interrupt();
                        break;
                    default:
                        System.out.println("Invalid command");
                        break;
                }
            } else if (input.equals("/")) {
                System.out.println("Invalid command");
            } else if (input.length() > 0) {
                if (socketOpen) {
                    node.broadcast(input);
                } else {
                    System.out.println("Create or join a network to send messages.");
                }
            }
        }
    }
}