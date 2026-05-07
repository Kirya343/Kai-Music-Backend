package org.kirya343.core.audio.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class RoomExecutorRegistry {

    private final Map<Long, ThreadPoolExecutor> executors = new ConcurrentHashMap<>();

    public ThreadPoolExecutor get(Long roomId) {
        return executors.computeIfAbsent(roomId, id -> {

            return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("room-executor-" + id);
                    return t;
                }
            );
        });
    }

    public Map<Long, ThreadPoolExecutor> getAll() {
        return executors;
    }
}