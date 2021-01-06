# Raft Message Broadcasting

An API for total order broadcast using the Raft algorithm. Includes a console application for manual testing.

## API

The node class is used both as the client and server depending on its role. A network then consists exclusively of nodes communicating via a single appointed leader node.

- To create a new network, instantiate a node, and call `createNetwork(int port)`. The node will be set as the leader node for the network.
- To connect to a network, instantiate a node, and call `connect(InetAddress address, int port)`. The node will connect and be set as a follower.
- To broadcast a message to all other nodes in the networ, call `broadcast(IMessage message)` (or use the overloaded `broadcast(TextMessage message)` to sent a simple text message).
- To disconnect from the network, simply call `disconnect()`.

In order to send a specific type of message, the `IMessage` interface can be extended to include new field. The node class will also need to be extended to recognise the new message type.

## Console App

The console app is started by default when running the program. It supports several default commands:

- `/create [port]`: Creates a network with an optional port.
- `/connect IpNetAddress port`: Connects to an existing network.
- `[message]`: If connected/hosting a network, broadcasts a message to all other nodes.
- `/disconnect`: Disconnects from the server and terminates the node.
