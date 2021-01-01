package maximwebb.app.server;

import maximwebb.app.client.Link;
import maximwebb.app.messages.IMessage;
import maximwebb.app.messages.TextMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class ClientHandler {
    private final Thread outgoingHandler;
    private final Thread incomingHandler;
    private final Socket socket;
    private MultiQueue multiQueue;
    private MessageQueue messageQueue;

    public ClientHandler(Socket s, MultiQueue mq) {
        socket = s;
        this.multiQueue = mq;
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
            while (true) {
                IMessage msg = messageQueue.take();
                oos.writeObject(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Receives messages from clients and adds to message queue */
    private void incoming() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            while (true) {
                try {
                    Object raw = ois.readObject();
                    if (raw instanceof IMessage) {
                        multiQueue.put((IMessage) raw);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}