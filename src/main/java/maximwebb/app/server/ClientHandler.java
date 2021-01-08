package maximwebb.app.server;

import maximwebb.app.client.Node;
import maximwebb.app.client.NodeInfo;
import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.NodeInfoMessage;
import maximwebb.app.messages.NodeInfoRequestMessage;
import maximwebb.app.messages.TextMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler {
    private final Thread outgoingHandler;
    private final Thread incomingHandler;
    private Node node;
    private final Socket socket;
    private final UUID serverId;
    private MultiQueue multiQueue;
    private MessageQueue messageQueue;
    private boolean shutdown = false;

    public ClientHandler(Node node, Socket socket, MultiQueue mq, UUID serverId) {
        this.node = node;
        this.socket = socket;
        this.multiQueue = mq;
        this.serverId = serverId;
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
            multiQueue.deregister(messageQueue);
            multiQueue.put(new TextMessage("User has disconnected.", "Server", serverId, null));
        }
    }

    /* Receives messages from clients and adds to message queue */
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
                            for (NodeInfo info : node.nodeInfoMap.values()) {
                                messageQueue.put(new NodeInfoMessage(info, authorId));
                            }
                        } else if (raw instanceof NodeInfoMessage) {
                            //TODO: specify recipient on multiqueue
                            multiQueue.put(((NodeInfoMessage) raw));
                        }
                    }
                } catch (IOException e) {
                    multiQueue.deregister(messageQueue);
                    multiQueue.put(new TextMessage("User has disconnected.", "Server", serverId, null));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        shutdown = true;
        multiQueue.deregister(messageQueue);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}