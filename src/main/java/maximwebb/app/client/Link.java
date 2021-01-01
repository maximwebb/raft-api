package maximwebb.app.client;

import maximwebb.app.messages.IMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Link {
    private Socket socket;
    private Node node;
    private Thread socketHandler;

    public Link(Node n, InetAddress address, int port) {
        node = n;
        try {
            socket = new Socket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketHandler = new Thread(this::run);
        socketHandler.setDaemon(true);
        socketHandler.start();
    }

    public Link(Node n, String address, int port) {
        try {
            new Link(n, InetAddress.getByName(address), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(IMessage message) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notify(IMessage message) {
        node.deliver(message);
    }

    public void run() {
        System.out.println("Opening connection.");
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
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

    public void shutdown() {
        System.out.println("Link terminated. Closing connection.");
        try {
            socketHandler.join();
            socket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
