package maximwebb.app.server;

import maximwebb.app.messages.TextMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class MessageServer {
    ServerSocket serverSocket;
    ArrayList<Socket> clients;
    ArrayList<ClientHandler> clientHandlers;

    public MessageServer(int port) {
        clients = new ArrayList<>();
        clientHandlers = new ArrayList<>();
        MultiQueue multiQueue = new MultiQueue();

        try {
            InetAddress address = InetAddress.getLocalHost();
            serverSocket = new ServerSocket(port, 10, address);
        } catch (IOException e) {
            System.out.println("Error connecting to port " + port);
            System.exit(-1);
        }
        System.out.println("Server connected to IP Address: " + serverSocket.getInetAddress() + " on port " + port);

        Thread clientAccepter = new Thread() {
            @Override
            public void run() {
                super.run();
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
                        clientHandlers.add(clientHandler);
                        multiQueue.put(new TextMessage(client.toString() + " connected from " + client.getInetAddress() + ".", UUID.randomUUID(), "Server"));
                    }
                }
            }
        };
//        clientAccepter.setDaemon(true);
        clientAccepter.start();
    }
}
