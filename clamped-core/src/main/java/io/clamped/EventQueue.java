package io.clamped;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, bounded in-memory queue for ClampedEvents.
 * When the queue is full, new events are silently dropped... Clamped never blocks or throws
 * in user code.
 */
public final class EventQueue {

    private final ConcurrentLinkedQueue<ClampedEvent> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final int maxSize;

    public EventQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Attempts to enqueue an event. Returns false and silently drops if the queue is full.
     */
    public boolean offer(ClampedEvent event) {
        if (size.get() >= maxSize) {
            return false;
        }
        if (queue.offer(event)) {
            size.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Retrieves and removes the head of the queue, or returns null if empty.
     */
    public ClampedEvent poll() {
        ClampedEvent event = queue.poll();
        if (event != null) size.decrementAndGet();
        return event;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return size.get();
    }
}
