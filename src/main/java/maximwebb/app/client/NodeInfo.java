package maximwebb.app.client;

import java.net.InetAddress;
import java.util.UUID;

public class NodeInfo {
    public InetAddress address;
    public int port;
    UUID networkId;
    UUID nodeId;
    NodeRole nodeRole;

    public NodeInfo(InetAddress address, int port, UUID networkId, UUID nodeId, NodeRole nodeRole) {
        this.address = address;
        this.port = port;
        this.networkId = networkId;
        this.nodeId = nodeId;
        this.nodeRole = nodeRole;
    }
}
