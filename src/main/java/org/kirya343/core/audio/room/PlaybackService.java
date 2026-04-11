package org.kirya343.core.audio.room;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.core.audio.AudioService;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.audio.RoomPlaybackEvent;
import org.kirya343.dto.auth.UserAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final AudioService audioService;
    private final QueueService queueService;
    private final RoomWebSocketService webSocketService;
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(4);
    private final ApplicationEventPublisher publisher;
    private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

    public void playNext(RoomState room, Long previousAudioId, UserAuthData authData) {

        logger.info("Переключаем песню в комнате {} на следующую", room.getRoomId());
        
        if (room.getTimer() != null) {
            room.getTimer().cancel(false);
        }

        QueueItem entry = queueService.nextTrack(room.getRoomId(), previousAudioId);

        loadRoomTrack(room, entry, authData);
    }

    public void playPrev(RoomState room, Long currentAudioId, UserAuthData authData) {

        logger.info("Переключаем песню в комнате {} на предыдущую", room.getRoomId());
        
        if (room.getTimer() != null) {
            room.getTimer().cancel(false);
        }

        QueueItem entry = queueService.prevTrack(room.getRoomId(), currentAudioId);

        loadRoomTrack(room, entry, authData);
    }

    private void loadRoomTrack(RoomState room, QueueItem entry, UserAuthData authData) {
        if (entry == null) {
            room.setCurrentQueueEntryId(null);
            room.setPaused(true);
            return;
        }

        long duration = audioService.getDuration(entry.getAudio().getPath());

        room.setCurrentQueueEntryId(entry.getId());
        room.setDuration(duration);
        room.setRemaining(duration);
        room.setPaused(false);

        ScheduledFuture<?> timer = scheduler.schedule(
            () -> playNext(room, room.getCurrentQueueEntryId(), authData),
            room.getRemaining(),
            TimeUnit.SECONDS
        );

        room.setTimer(timer);

        PlaybackStateDTO checked = new PlaybackStateDTO("Server", room.getCurrentQueueEntryId(), Long.valueOf(0), false);

        publisher.publishEvent(
            new RoomPlaybackEvent(room.getRoomId(), checked.entryId(), checked.position(), checked.pause(), authData)
        );
 
        webSocketService.broadcastPlaybackState(room.getRoomId(), checked);
    }
}