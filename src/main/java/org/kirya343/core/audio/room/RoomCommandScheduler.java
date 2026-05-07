package org.kirya343.core.audio.room;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kirya343.dto.room.commands.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomCommandScheduler {

    private final RoomStateStore rooms;
    private final RoomCommandWorker roomCommandWorker;
    private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void start() {
        logger.debug("Включаем таймер тиков комнаты");

        scheduler.scheduleAtFixedRate(() -> {

            logger.debug("Таймер на тик комнаты сработал, команты: {}", rooms.roomIds().size());

            long now = System.nanoTime();

            for (Long roomId : rooms.roomIds()) {
                roomCommandWorker.submit(new Tick(roomId, now));
            }

        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
    }
}