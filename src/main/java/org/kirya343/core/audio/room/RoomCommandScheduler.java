package org.kirya343.core.audio.room;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kirya343.dto.room.commands.Tick;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomCommandScheduler {

    private final RoomStateStore rooms;
    private final RoomCommandQueue queue;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {

            long now = System.nanoTime();

            for (Long roomId : rooms.roomIds()) {
                queue.submit(new Tick(roomId, now));
            }

        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
    }
}