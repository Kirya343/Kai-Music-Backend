package org.kirya343.infrastructure.diagnostics.logging;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Deque;

import org.springframework.stereotype.Component;

@Component
public class LogBuffer {

    private static final int MAX_SIZE = 1000;

    private final Deque<String> logs = new ArrayDeque<>();

    public synchronized void add(String message) {
        if (logs.size() >= MAX_SIZE) {
            logs.removeFirst();
        }

        logs.addLast(message);
    }

    public synchronized List<String> getLogs() {
        return List.copyOf(logs);
    }

    public synchronized void clear() {
        logs.clear();
    }
}