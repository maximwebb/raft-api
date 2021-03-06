package maximwebb.app.client;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.NodeInfoMessage;
import maximwebb.app.messages.NodeInfoRequestMessage;
import maximwebb.app.messages.StatusMessage;
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
    private Thread leaderTimeoutListener;
    private long leaderTimeoutDuration;
    private boolean willExit;
    private boolean leaderAlive;

    /* Leader behaviour handling */
    public ServerSocket serverSocket;
    private Thread clientAccepterHandler;
    public Thread heartbeatHandler;
    private ArrayList<ClientHandler> clientHandlerList;
    private MultiQueue multiQueue;

    /* Election and consistency handling */
    public HashMap<UUID, NodeInfo> nodeInfoMap;
    private ArrayList<IMessage> log = null;

    public Node(String name) {
        this.name = name;
        uid = UUID.randomUUID();
        willExit = false;
        leaderTimeoutDuration = (long) ((Math.random() * 7 + 3) * 1000);
        System.out.println("Timeout duration set to: " + leaderTimeoutDuration);
    }

    public void connect(InetAddress address, int port) throws IOException {
        this.role = NodeRole.FOLLOWER;
        willExit = false;
        leaderAlive = true;
        nodeInfoMap = new HashMap<>();
        datagramSocket = new DatagramSocket(0);
        nodeSocket = new Socket(address, port);
        incomingMessageHandler = new Thread(this::incomingHandler);
        incomingMessageHandler.setDaemon(true);
        incomingMessageHandler.start();
        directMessageHandler = new Thread(this::receiveDirectMessage);
        directMessageHandler.setDaemon(true);
        directMessageHandler.start();
        broadcast(new NodeInfoMessage(getNodeInfo(), null));
        broadcast(new NodeInfoRequestMessage(uid));
        leaderTimeoutListener = new Thread(this::testLeaderTimeout);
        leaderTimeoutListener.setDaemon(true);
        leaderTimeoutListener.start();
    }

    public void disconnect() {
        if (!willExit) {
            willExit = true;
            incomingMessageHandler.interrupt();
            //TODO: Create custom disconnect message
            broadcast(name + " disconnected.");
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
        heartbeatHandler = new Thread(this::heartbeat);
        heartbeatHandler.setDaemon(true);
        heartbeatHandler.start();
    }

    public InetAddress getAddress() {
        return datagramSocket.getInetAddress();
    }

    public int getPort() {
        return datagramSocket.getLocalPort();
    }

    private void incomingHandler() {
        System.out.println("Opening connection.");
        try {
            ObjectInputStream ois = new ObjectInputStream(nodeSocket.getInputStream());
            while (!willExit) {
                Object raw = ois.readObject();
                if (raw instanceof IMessage) {
                    leaderAlive = true;
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
                broadcast(new NodeInfoMessage(getNodeInfo(), null));
            }
        }
    }

    public void broadcast(IMessage message) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(nodeSocket.getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String msg) {
        TextMessage textMessage = new TextMessage(msg, name, uid, null);
        broadcast(textMessage);
    }

    void deliver(IMessage message) {
        if (message instanceof TextMessage) {
            System.out.println(message.toString());
        } else if (message instanceof NodeInfoMessage) {
            NodeInfo info = ((NodeInfoMessage) message).nodeInfo;
            setNodeInfo(info);
        } else if (message instanceof NodeInfoRequestMessage) {
            broadcast(new NodeInfoMessage(getNodeInfo(), null));
        } else if (message instanceof StatusMessage) {
            System.out.println("Heartbeat received - resetting timer.");
        }
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
                //TODO: implement this
                System.err.println("Message corrupted. Requesting again...");
            }
        }
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(getAddress(), getPort(), networkId, uid, name, role);
    }

    public void setNodeInfo(NodeInfo nodeInfo) {
        if (nodeInfo.nodeId.equals(uid)) {
            this.name = nodeInfo.name;
            this.networkId = nodeInfo.networkId;
            this.role = nodeInfo.nodeRole;
        }
        nodeInfoMap.put(nodeInfo.nodeId, nodeInfo);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        nodeInfoMap.get(uid).setName(name);
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

    private void testLeaderTimeout() {
        while (leaderAlive) {
            leaderAlive = false;
            try {
                Thread.sleep(leaderTimeoutDuration);
            } catch (InterruptedException e) {
                System.out.println("Timeout thread interrupted.");
            }
        }
        System.out.println("Leader timeout detected! Triggering leader election...");
    }

    private void heartbeat() {
        try {
            while (true) {
                Thread.sleep(1000);
                broadcast(new StatusMessage(uid));
            }
        } catch (InterruptedException e) {
            System.out.println("Heartbeat thread interrupted.");
        }
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
