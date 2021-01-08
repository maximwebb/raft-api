package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.NodeInfoMessage;
import maximwebb.app.messages.NodeInfoRequestMessage;
import maximwebb.app.messages.TextMessage;
import maximwebb.app.server.ClientHandler;
import maximwebb.app.server.MultiQueue;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Node {
    /* General information */
    public UUID uid;
    public UUID networkId;
    public NodeRole role;
    public String name;

    /* Follower behaviour handling */
    private Socket nodeSocket;
    private DatagramSocket datagramSocket;
    private Thread incomingMessageHandler;
    private Thread directMessageHandler;
    private boolean willExit;

    /* Leader behaviour handling */
    public ServerSocket serverSocket;
    private Thread clientAccepterHandler;
    private ArrayList<ClientHandler> clientHandlerList;
    private MultiQueue multiQueue;

    /* Election and consistency handling */
    public HashMap<UUID, NodeInfo> nodeInfoMap;
    private ArrayList<IMessage> log = null;

    public Node(String name) {
        this.name = name;
        uid = UUID.randomUUID();
        willExit = false;
    }

    public void connect(InetAddress address, int port) throws IOException {
        this.role = NodeRole.FOLLOWER;
        willExit = false;
        nodeInfoMap = new HashMap<>();
        datagramSocket = new DatagramSocket(0);
        nodeSocket = new Socket(address, port);
        incomingMessageHandler = new Thread(this::incomingHandler);
        incomingMessageHandler.setDaemon(true);
        incomingMessageHandler.start();
        directMessageHandler = new Thread(this::receiveDirectMessage);
        directMessageHandler.setDaemon(true);
        directMessageHandler.start();
        broadcast(new NodeInfoMessage(getNodeInfo(), uid));
        broadcast(new NodeInfoRequestMessage(uid));
    }

    public void disconnect() {
        if (!willExit) {
            willExit = true;
            incomingMessageHandler.interrupt();
            //TODO: Create custom disconnect message
            broadcast(name + " disconnected.");
        }
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
        } catch (EOFException e) {
            System.out.println("Server node terminated.");
            willExit = true;
            incomingMessageHandler.interrupt();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        nodeInfoMap = new HashMap<>();
        try {
            serverSocket = new ServerSocket(port);
            nodeSocket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
            datagramSocket = new DatagramSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        clientHandlerList = new ArrayList<>();
        multiQueue = new MultiQueue();

        incomingMessageHandler = new Thread(this::incomingHandler);
        incomingMessageHandler.setDaemon(true);
        incomingMessageHandler.start();
        directMessageHandler = new Thread(this::receiveDirectMessage);
        directMessageHandler.setDaemon(true);
        directMessageHandler.start();
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
                ClientHandler clientHandler = new ClientHandler(this, client, multiQueue, uid);
                clientHandlerList.add(clientHandler);
                multiQueue.put(new TextMessage(client.toString() + " connected from " + client.getInetAddress() + ".", "Server", uid, null));
                multiQueue.put(new NodeInfoMessage(getNodeInfo(), null));
            }
        }
    }

    public InetAddress getAddress() {
        return datagramSocket.getInetAddress();
    }

    public int getPort() {
        return datagramSocket.getLocalPort();
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

    public void sendDirectMessage(IMessage message) {
        if (message.getRecipientId() != null) {
            NodeInfo node = nodeInfoMap.get(message.getRecipientId());
            try {
                byte[] buffer;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                buffer = baos.toByteArray();
                InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
                System.out.println("Sending DM to: ");
                System.out.println(inetAddress);
                System.out.println(node.port);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, node.port);
                datagramSocket.send(packet);
            } catch (IOException e) {
                System.err.println("Error sending direct message.");
            }
        } else {
            System.out.println("Error: Please specify recipient...");
        }
    }


    private void receiveDirectMessage() {
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(packet);
                buffer = packet.getData();
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                ObjectInputStream ois = new ObjectInputStream(bais);
                IMessage o = (IMessage) ois.readObject();
                if (o != null) {
                    deliver(o);
                } else {
                    throw new ClassNotFoundException();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Message corrupted. Requesting again...");
            }

        }
    }

    void deliver(IMessage message) {
        if (message instanceof TextMessage) {
            System.out.println(message.toString());
        } else if (message instanceof NodeInfoMessage) {
            NodeInfo info = ((NodeInfoMessage) message).nodeInfo;
            if (info.nodeId != uid) {
                nodeInfoMap.put(info.nodeId, info);
                System.out.println("Added " + info.name + " to " + name);
            }
        }
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(getAddress(), getPort(), networkId, uid, name, role);
    }


    public Set<NodeInfo> getNodesByName(String name) {
        Set<NodeInfo> result = new HashSet<>();
        for (NodeInfo info : nodeInfoMap.values()) {
            if (info.name.equals(name)) {
                result.add(info);
            }
        }
        return result;
    }

    public NodeInfo getNodeById(UUID id) {
        return nodeInfoMap.get(id);
    }

    public boolean hasNodeId(UUID nodeId) {
        return nodeInfoMap.containsKey(nodeId);
    }

    public void promote() {
        clientHandlerList = new ArrayList<>();
        multiQueue = new MultiQueue();
        for (NodeInfo info : nodeInfoMap.values()) {
            try {
                Socket s = new Socket(info.address, info.port);
                ClientHandler clientHandler = new ClientHandler(this, s, multiQueue, uid);
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
