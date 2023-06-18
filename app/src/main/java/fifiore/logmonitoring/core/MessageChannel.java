package fifiore.logmonitoring.core;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class MessageChannel<T> {

    private final BlockingQueue<Optional<T>> queue = new LinkedBlockingQueue<>();

    void push(T message) {
        try {
            queue.put(Optional.of(message));
        } catch (InterruptedException exception) {
            LogStream.err(exception);
            Thread.currentThread().interrupt();
        }
    }

    Optional<T> read() {
        try {
            return queue.take();
        } catch (InterruptedException exception) {
            LogStream.err(exception);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    void close() {
        try {
            queue.put(Optional.empty());
        } catch (InterruptedException exception) {
            LogStream.err(exception);
            Thread.currentThread().interrupt();
        }
    }
}
