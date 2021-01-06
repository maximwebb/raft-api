package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;
import maximwebb.app.server.ClientHandler;
import maximwebb.app.server.MultiQueue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class Node {
    /* General information */
    public UUID uid;
    public UUID networkId;
    public NodeRole role;
    public String name;

    /* Follower behaviour handling */
    private Socket nodeSocket;
    private Thread incomingMessageHandler;
    private boolean willExit;

    /* Leader behaviour handling */
    private ServerSocket serverSocket;
    private Thread clientAccepterHandler;
    private ArrayList<ClientHandler> clientHandlerList;
    private MultiQueue multiQueue;

    /* Election and consistency handling */
    private HashSet<NodeInfo> nodeInfoSet;
    private ArrayList<IMessage> log = null;

    public Node(String name) {
        this.name = name;
        uid = UUID.randomUUID();
        willExit = false;
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(getAddress(), getPort(), networkId, uid, role);
    }

    public void connect(InetAddress address, int port) throws IOException {
        this.role = NodeRole.FOLLOWER;
        nodeInfoSet = new HashSet<>();
        nodeSocket = new Socket(address, port);
        serverSocket = new ServerSocket(0);
        incomingMessageHandler = new Thread(this::incomingHandler);
        incomingMessageHandler.setDaemon(true);
        incomingMessageHandler.start();
    }

    private void incomingHandler() {
        System.out.println("Opening connection.");
        try {
            ObjectInputStream ois = new ObjectInputStream(nodeSocket.getInputStream());
            while (!willExit) {
                Object raw = ois.readObject();
                if (raw instanceof IMessage) {
                    deliver((IMessage) raw);
                } else {
                    System.out.println("Invalid message type.");
                }
            }
            System.out.println("Stopped thread.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        willExit = true;
        incomingMessageHandler.interrupt();
        //TODO: Create custom disconnect message
        broadcast(name + " disconnected.");
    }

    public void createNetwork(int port) {
        try {
            if (nodeSocket != null && !nodeSocket.isClosed()) {
                nodeSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error when closing socket.");
        }

        this.role = NodeRole.LEADER;
        nodeInfoSet = new HashSet<>();
        try {
            serverSocket = new ServerSocket(port);
            nodeSocket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

        clientHandlerList = new ArrayList<>();
        multiQueue = new MultiQueue();

        incomingMessageHandler = new Thread(this::incomingHandler);
        incomingMessageHandler.setDaemon(true);
        incomingMessageHandler.start();
        clientAccepterHandler = new Thread(this::clientAccepter);
        clientAccepterHandler.setDaemon(true);
        clientAccepterHandler.start();
    }

    private void clientAccepter() {
        while (true) {
            Socket client = null;
            try {
                client = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (client != null) {
                ClientHandler clientHandler = new ClientHandler(client, multiQueue, uid);
                clientHandlerList.add(clientHandler);
                multiQueue.put(new TextMessage(client.toString() + " connected from " + client.getInetAddress() + ".", "Server", uid, null));
            }
        }
    }

    public InetAddress getAddress() {
        return serverSocket.getInetAddress();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void broadcast(IMessage message) {
        if (role == NodeRole.LEADER) {
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

    public void broadcast(String msg) {
        TextMessage textMessage = new TextMessage(msg, name, uid, null);
        broadcast(textMessage);
    }

    void deliver(IMessage message) {
        if (message instanceof TextMessage) {
            System.out.println(message.toString());
        }
    }

    public void promote() {
        clientHandlerList = new ArrayList<>();
        multiQueue = new MultiQueue();
        for (NodeInfo info : nodeInfoSet) {
            try {
                Socket s = new Socket(info.address, info.port);
                ClientHandler clientHandler = new ClientHandler(s, multiQueue, uid);
                clientHandlerList.add(clientHandler);
            } catch (IOException e) {
                System.out.println("Error: could not connect to client at " + info.address + " on port " + info.port + ".");
            }
        }
        clientAccepterHandler = new Thread(this::clientAccepter);
        clientAccepterHandler.start();
        // TODO: Make custom PromotionMessage class
        multiQueue.put(new TextMessage("Node " + uid + " has been promoted to leader.", "Server", uid, null));
    }

    public void demote() {
        role = NodeRole.FOLLOWER;
        try {
            clientAccepterHandler.join();
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.shutdown();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
