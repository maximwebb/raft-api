package maximwebb.app.server;

import maximwebb.app.messages.IMessage;

import java.util.HashSet;
import java.util.Set;

public class MultiQueue {
    Set<MessageQueue> messageQueues;

    public MultiQueue() {
        messageQueues = new HashSet<>();
    }

    public synchronized void register(MessageQueue q) {
        messageQueues.add(q);
    }

    public synchronized void deregister(MessageQueue q) {
        messageQueues.remove(q);
    }

    public synchronized void put(IMessage message) {
        for (MessageQueue queue : messageQueues) {
            queue.put(message);
        }
        this.notifyAll();
    }
}
