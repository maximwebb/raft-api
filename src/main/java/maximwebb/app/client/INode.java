package maximwebb.app;

import java.util.ArrayList;
import java.util.UUID;

public interface INode {
    public void connect(UUID nId);
    public void disconnect();
    public void broadcast(IMessage msg);
}
