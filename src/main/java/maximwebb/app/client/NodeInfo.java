package maximwebb.app.client;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public class NodeInfo implements Serializable {
    public InetAddress address;
    public int port;
    public UUID networkId;
    public UUID nodeId;
    public String name;
    public NodeRole nodeRole;

    public NodeInfo(InetAddress address, int port, UUID networkId, UUID nodeId, String name, NodeRole nodeRole) {
        this.address = address;
        this.port = port;
        this.networkId = networkId;
        this.nodeId = nodeId;
        this.name = name;
        this.nodeRole = nodeRole;
    }

    public void setName(String name) {
        this.name = name;
    }
}
