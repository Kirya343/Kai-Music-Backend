package org.kirya343.core.audio.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.core.audio.AudioService;
import org.kirya343.datasource.model.user.audio.AudioFile;
import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.kirya343.datasource.model.user.audio.QueueItem;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomManager {

    private final Map<Long, RoomState> rooms = new ConcurrentHashMap<>();
    private final PlaybackService playbackService;
    private final AudioService audioService;
    private final RoomWebSocketService webSocketService;
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(4);

    private final ListeningRoomRepository listeningRoomRepository;
    private final QueueItemRepository queueItemRepository;
    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    public RoomState getRoom(Long roomId, Long currentTrackId) {
        return rooms.computeIfAbsent(roomId, id -> createRoomState(id, currentTrackId));
    }

    public void playTrack(Long roomId, PlaybackStateDTO state) {
        logger.info("Включаем трек {} в комнате {}", state.audioId(), roomId);

        RoomState room = getRoom(roomId, state.audioId());

        if (room.getCurrentTrackId() != state.audioId()) {
            // TODO переключить трек в RoomState и обновить данные

            QueueItem qi = queueItemRepository.findByRoomIdAndAudioId(roomId, state.audioId()).orElseThrow(
                () -> new EntityNotFoundException("Трек не найден в очереди"));

            AudioFile audio = qi.getAudio();
            Long duration = audioService.getDuration(audio.getPath());

            room.setDuration(duration);
        }

        logger.info("Длительность {}, позиция {}, осталось {}", room.getDuration(), state.position(), room.getDuration() - state.position());
        room.setRemaining(room.getDuration() - state.position());
        room.setPaused(false);

        ScheduledFuture<?> oldTimer = room.getTimer();
        if (oldTimer != null && !oldTimer.isDone()) {
            logger.info("Отменяем старый таймер трека {} в комнате {}", state.audioId(), roomId);
            oldTimer.cancel(false);
        }

        ScheduledFuture<?> timer = scheduler.schedule(
            () -> {
                logger.info("🔥 Таймер сработал для трека {} в комнате {}", state.audioId(), roomId);
                playbackService.playNext(room, state.audioId());
            },
            room.getRemaining(),
            TimeUnit.SECONDS
        );

        logger.info("Таймер запланирован: delay={}ms, isDone={}", room.getRemaining(), timer.isDone());
        room.setTimer(timer);

        PlaybackStateDTO checked = new PlaybackStateDTO(room.getCurrentTrackId(), state.position(), false);

        webSocketService.broadcastPlaybackState(roomId, checked);
    }

    public void pause(Long roomId, PlaybackStateDTO state) {

        logger.info("Ставим на паузу трек {} в комнате {}", state.audioId(), roomId);
        logger.info(rooms.toString());

        RoomState room = getRoom(roomId, state.audioId());

        if (room.getTimer() != null) {
            room.getTimer().cancel(false);
        }

        room.setPaused(true);

        PlaybackStateDTO checked = new PlaybackStateDTO(room.getCurrentTrackId(), state.position(), true);

        webSocketService.broadcastPlaybackState(roomId, checked);
    }


    private RoomState createRoomState(Long roomId, Long currentTrackId) {

        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("комната не найдена"));

        AudioFile audio = queueItemRepository.findAudioInRoomQueue(roomId, currentTrackId).orElseThrow(
            () -> new EntityNotFoundException("Трек не найден в очереди комнаты"));

        Long duration = audioService.getDuration(audio.getPath());

        return new RoomState(
            room.getId(),
            audio.getId(),
            duration,
            duration,
            false
        );
    }
}
