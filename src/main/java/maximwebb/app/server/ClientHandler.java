package maximwebb.app.server;

import maximwebb.app.client.Node;
import maximwebb.app.client.NodeInfo;
import maximwebb.app.messages.ChangeNameMessage;
import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.NodeInfoMessage;
import maximwebb.app.messages.NodeInfoRequestMessage;
import maximwebb.app.messages.TextMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler {
    private final Thread outgoingHandler;
    private final Thread incomingHandler;
    private final Node serverNode;
    private final Socket socket;
    private final UUID serverId;
    private UUID clientId;
    private MultiQueue multiQueue;
    private MessageQueue messageQueue;
    private boolean shutdown = false;

    public ClientHandler(Node serverNode, Socket socket, MultiQueue mq, UUID serverId) {
        this.serverNode = serverNode;
        this.socket = socket;
        this.multiQueue = mq;
        this.serverId = serverId;
        this.clientId = null;
        messageQueue = new MessageQueue();
        multiQueue.register(messageQueue);

        outgoingHandler = new Thread(this::outgoing);
        outgoingHandler.setDaemon(true);
        outgoingHandler.start();

        incomingHandler = new Thread(this::incoming);
        incomingHandler.setDaemon(true);
        incomingHandler.start();
    }

    /* Sends messages to clients from message queue */
    private void outgoing() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            while (!shutdown) {
                IMessage msg = messageQueue.take();
                oos.writeObject(msg);
            }
        } catch (IOException e) {
            multiQueue.deregister(clientId, messageQueue);
            multiQueue.put(new TextMessage("User has disconnected.", "Server", serverId, null));
        }
    }

    /* Receives messages from clients and adds to multi-queue */
    private void incoming() {
        try {
            while (!shutdown) {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                try {
                    Object raw = ois.readObject();
                    if (raw instanceof IMessage) {
                        UUID authorId = ((IMessage) raw).getAuthorId();
                        if (raw instanceof TextMessage) {
                            multiQueue.put((IMessage) raw);
                        } else if (raw instanceof NodeInfoRequestMessage) {
                            for (NodeInfo info : serverNode.nodeInfoMap.values()) {
                                multiQueue.put(new NodeInfoMessage(info, authorId));
                            }
                        } else if (raw instanceof NodeInfoMessage) {
                            NodeInfoMessage infoMessage = (NodeInfoMessage) raw;
                            if (getClientId() == null) {
                                setClientId(infoMessage.nodeInfo.nodeId);
                                multiQueue.put(new TextMessage(infoMessage.nodeInfo.name + " has connected.", "Server", serverId, null));
                            }
                            multiQueue.put(infoMessage);
                        } else if (raw instanceof ChangeNameMessage) {
                            if (getClientId() == null) {
                                multiQueue.put(new TextMessage("Error setting name.", "Server", serverId, authorId));
                                multiQueue.put(new NodeInfoRequestMessage(authorId));
                            } else {
                                String newName = ((ChangeNameMessage) raw).name;
                                NodeInfo nodeInfo = serverNode.getNodeById(authorId);
                                String oldName = nodeInfo.name;
                                NodeInfo newNodeInfo = new NodeInfo(nodeInfo.address, nodeInfo.port, nodeInfo.networkId, nodeInfo.nodeId, newName, nodeInfo.nodeRole);
                                multiQueue.put(new NodeInfoMessage(newNodeInfo, null));
                                multiQueue.put(new TextMessage(oldName + " has changed their name to " + newName, "Server", serverId, null));
                            }
                        }
                    }
                } catch (IOException e) {
                    multiQueue.deregister(clientId, messageQueue);
                    multiQueue.put(new TextMessage("User has disconnected.", "Server", serverId, null));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (EOFException e) {
            multiQueue.deregister(getClientId(), messageQueue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UUID getClientId() {
        return clientId;
    }

    private void setClientId(UUID uuid) {
        if (clientId == null) {
            clientId = uuid;
            multiQueue.transfer(uuid, messageQueue);
        }
    }

    public void shutdown() {
        shutdown = true;
        multiQueue.deregister(clientId, messageQueue);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}