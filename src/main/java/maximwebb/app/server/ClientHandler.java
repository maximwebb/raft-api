package maximwebb.app.server;

import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler {
    private final Thread outgoingHandler;
    private final Thread incomingHandler;
    private final Socket socket;
    private final UUID serverId;
    private MultiQueue multiQueue;
    private MessageQueue messageQueue;
    private boolean shutdown = false;

    public ClientHandler(Socket s, MultiQueue mq, UUID serverId) {
        socket = s;
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
            System.out.println("test");
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
                        multiQueue.put((IMessage) raw);
                    }
                } catch (IOException e) {
                    System.out.println("test");
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