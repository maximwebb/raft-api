package maximwebb.app.client;

import maximwebb.app.messages.IMessage;

import java.net.InetAddress;
import java.util.UUID;

public interface INode {
    public void connect(InetAddress address, int port);
    public void disconnect() throws InterruptedException;
    public void broadcast(IMessage msg);
}
