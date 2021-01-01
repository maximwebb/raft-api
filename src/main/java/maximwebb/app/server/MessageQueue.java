package maximwebb.app.server;

import maximwebb.app.messages.IMessage;

import java.util.LinkedList;
import java.util.Queue;

class MessageQueue {
        Queue<IMessage> messages;

        MessageQueue() {
            messages = new LinkedList<>();
        }

        public synchronized void put(IMessage message) {
            messages.add(message);
            this.notifyAll();
        }

        public synchronized IMessage take() {
            while (messages.isEmpty()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted.");
                }
            }
            return messages.poll();
        }
    }
