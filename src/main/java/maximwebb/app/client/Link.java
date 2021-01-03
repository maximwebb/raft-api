package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;
import maximwebb.app.server.ClientHandler;
import maximwebb.app.server.MultiQueue;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class Link {
    private Node node;
    private HashSet<NodeInfo> nodeInfoSet;
    private Socket nodeSocket;
    private Thread nodeIncomingHandler;
    private ServerSocket serverSocket;
    private Thread clientAccepterHandler;
    private ArrayList<Socket> clients;
    private ArrayList<ClientHandler> clientHandlerList;
    private MultiQueue multiQueue;

    public Link(Node n, InetAddress address, int port, NodeRole role) {
        node = n;
        nodeInfoSet = new HashSet<>();
        try {
            nodeSocket = new Socket(address, port);
            if (role == NodeRole.LEADER) {
                serverSocket = new ServerSocket(port);
                n.networkId = UUID.randomUUID();
                promote();
            } else {
                serverSocket = new ServerSocket(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        nodeIncomingHandler = new Thread(this::incomingHandler);
        nodeIncomingHandler.setDaemon(true);
        nodeIncomingHandler.start();
    }

    /* Used for initialising a node as leader */
    public Link(Node n) {
        n.networkId = UUID.randomUUID();
        node = n;
        nodeInfoSet = new HashSet<>();
        try {
            serverSocket = new ServerSocket(0);
            nodeSocket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        nodeIncomingHandler = new Thread(this::incomingHandler);
        nodeIncomingHandler.setDaemon(true);
        nodeIncomingHandler.start();
        promote();
    }

    public void broadcast(IMessage message) {
        if (node.role == NodeRole.LEADER) {
            multiQueue.put(message);
        } else {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(nodeSocket.getOutputStream());
                oos.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void notify(IMessage message) {
        node.deliver(message);
    }

    /* Promotes node from follower/candidate to leader */
    public void promote() {
        clients = new ArrayList<>();
        clientHandlerList = new ArrayList<>();
        multiQueue = new MultiQueue();
        for (NodeInfo info : nodeInfoSet) {
            try {
                Socket s = new Socket(info.address, info.port);
                clients.add(s);
                ClientHandler clientHandler = new ClientHandler(s, multiQueue);
                clientHandlerList.add(clientHandler);
            } catch (IOException e) {
                System.out.println("Error: could not connect to client at " + info.address + " on port " + info.port + ".");
            }
        }
        clientAccepterHandler = new Thread(this::clientAccepter);
        clientAccepterHandler.start();
        // TODO: Make custom PromotionMessage class
        multiQueue.put(new TextMessage("Node " + node.uid + " has been promoted to leader.", "Server", node.uid, null));
    }

    /* Demotes node from candidate/leader to follower */
    public void demote() {
        try {
            clientAccepterHandler.join();
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.shutdown();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        System.out.println("Link terminated. Closing connection.");
        try {
            nodeIncomingHandler.join();
            nodeSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* Thread is run regardless of node role. */
    public void incomingHandler() {
        System.out.println("Opening connection.");
        try {
            ObjectInputStream ois = new ObjectInputStream(nodeSocket.getInputStream());
            while (true) {
                Object raw = ois.readObject();
                if (raw instanceof IMessage) {
                    notify((IMessage) raw);
                } else {
                    System.out.println("Invalid message type.");
                }
            }
        } catch (InterruptedIOException e) {
            shutdown();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /* Thread is only run when node is promoted to leader role. */
    public void clientAccepter() {
        while (true) {
            Socket client = null;
            try {
                client = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (client != null) {
                clients.add(client);
                ClientHandler clientHandler = new ClientHandler(client, multiQueue);
                clientHandlerList.add(clientHandler);
                multiQueue.put(new TextMessage(client.toString() + " connected from " + client.getInetAddress() + ".", "Server", node.uid, null));
            }
        }
    }

    public InetAddress getAddress() {
        return serverSocket.getInetAddress();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }
}
