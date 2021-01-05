package maximwebb.app.ui;

import maximwebb.app.client.Node;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

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
                if (command.equals("create") || command.equals("createNetwork")) {
                    int port = args.length > 1 ? Integer.parseInt(args[1]) : 0;
                    node.createNetwork(port);
                    socketOpen = true;
                    System.out.println("Created network on address " + node.getAddress() + ", port: " + node.getPort());
                } else if (command.equals("connect")) {
                    if (args.length == 3 && StringUtils.isNumeric(args[2])) {
//                    if (args.length == 3) {
                        try {
                            node.connect(InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
                            socketOpen = true;
                        } catch (IOException e) {
                            System.out.println("Could not connect to " + args[1]);
                        }
                    } else {
                        System.out.println("Usage: /connect [InetAddress] [port]");
                    }
                } else if (command.equals("disconnect")) {
                    node.disconnect();
                    socketOpen = false;
                } else {
                    System.out.println("Invalid command");
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