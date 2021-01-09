package maximwebb.app.server;

import maximwebb.app.messages.IMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class MultiQueue {
    HashMap<UUID, MessageQueue> messageQueues;
    HashSet<MessageQueue> unclassifiedMessageQueues;

    public MultiQueue() {
        messageQueues = new HashMap<>();
        unclassifiedMessageQueues = new HashSet<>();
    }

    public synchronized void register(MessageQueue q) {
        unclassifiedMessageQueues.add(q);
    }

    public synchronized void register(UUID uuid, MessageQueue q) {
        messageQueues.put(uuid, q);
    }

    public synchronized void deregister(UUID uuid, MessageQueue q) {
        messageQueues.remove(uuid);
        unclassifiedMessageQueues.remove(q);
    }

    public synchronized void transfer(UUID uuid, MessageQueue q) {
        deregister(uuid, q);
        register(uuid, q);
    }

    public synchronized void put(IMessage message) {
        UUID recipientId = message.getRecipientId();
        if (recipientId == null) {
            for (MessageQueue queue : messageQueues.values()) {
                queue.put(message);
            }
            for (MessageQueue queue : unclassifiedMessageQueues) {
                queue.put(message);
            }
        } else {
            messageQueues.get(recipientId).put(message);
        }
        this.notifyAll();
    }

    public synchronized void putAllExcept(IMessage message, UUID uuid) {
        for (UUID recipientId : messageQueues.keySet()) {
            if (!recipientId.equals(uuid)) {
                messageQueues.get(recipientId).put(message);
            }
        }
        for (MessageQueue queue : unclassifiedMessageQueues) {
            queue.put(message);
        }
        this.notifyAll();
    }
}
