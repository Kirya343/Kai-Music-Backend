package org.kirya343.core.audio.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.core.audio.AudioService;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.RoomPlaybackState;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.datasource.repository.audio.RoomPlaybackStateRepository;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.audio.RoomPlaybackEvent;
import org.kirya343.dto.auth.UserAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
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
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final EntityManager entityManager;

    private final ApplicationEventPublisher publisher;

    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    public RoomState findRoom(Long roomId, Long currentTrackId) {
        return rooms.computeIfAbsent(roomId, id -> createRoomState(id, currentTrackId));
    }

    public RoomState getRoom(Long roomId) {
        return rooms.get(roomId);
    }

    public void playTrack(Long roomId, PlaybackStateDTO state, UserAuthData authData) {
        logger.info("Включаем трек {} в комнате {}", state.entryId(), roomId);

        RoomState initialRoom = findRoom(roomId, state.entryId());
        
        RoomState room = syncRoomState(initialRoom, state);

        logger.info("Длительность {}, позиция {}, осталось {}",
            room.getDuration(), state.position(), room.getDuration() - state.position()
        );

        room.setRemaining(room.getDuration() - state.position());
        room.setPaused(false);

        ScheduledFuture<?> oldTimer = room.getTimer();
        if (oldTimer != null && !oldTimer.isDone()) {
            logger.info("Отменяем старый таймер трека {} в комнате {}", state.entryId(), roomId);
            oldTimer.cancel(false);
        }

        ScheduledFuture<?> timer = scheduler.schedule(
            () -> {
                logger.info("🔥 Таймер сработал для трека {} в комнате {}", state.entryId(), roomId);
                playbackService.playNext(room, state.entryId(), authData);
            },
            room.getRemaining(),
            TimeUnit.SECONDS
        );

        logger.info("Таймер запланирован: delay={}ms, isDone={}", room.getRemaining(), timer.isDone());
        room.setTimer(timer);

        PlaybackStateDTO checked = new PlaybackStateDTO(authData.name(), room.getCurrentQueueEntryId(), state.position(), false);

        updatePlayback(roomId, checked, authData);

        webSocketService.broadcastPlaybackState(roomId, checked);
    }

    public void pause(Long roomId, PlaybackStateDTO state, UserAuthData authData) {

        logger.info("Ставим на паузу трек {} в комнате {}", state.entryId(), roomId);
        logger.info(rooms.toString());

        RoomState room = findRoom(roomId, state.entryId());

        room = syncRoomState(room, state);

        if (room.getTimer() != null) {
            room.getTimer().cancel(false);
        }

        room.setPaused(true);

        PlaybackStateDTO checked = new PlaybackStateDTO(authData.name(), room.getCurrentQueueEntryId(), state.position(), true);

        updatePlayback(roomId, checked, authData);

        webSocketService.broadcastPlaybackState(roomId, checked);
    }

    private RoomState createRoomState(Long roomId, Long currentQueueEntryId) {

        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("комната не найдена"));

        AudioFile audio = queueItemRepository.findAudioInRoomQueue(roomId, currentQueueEntryId).orElseThrow(
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

    private RoomState syncRoomState(RoomState room, PlaybackStateDTO state) {
        if (room.getCurrentQueueEntryId() != state.entryId()) {
            // TODO переключить трек в RoomState и обновить данные

            AudioFile audio = queueItemRepository.findAudioById(state.entryId());

            Long duration = audioService.getDuration(audio.getPath());

            room.setCurrentQueueEntryId(state.entryId());
            room.setDuration(duration);
        }
        return room;
    }

    public void updatePlayback(Long roomId, PlaybackStateDTO state, UserAuthData authData) {

        publisher.publishEvent(
            new RoomPlaybackEvent(roomId, state.entryId(), state.position(), state.pause(), authData)
        );
    }

    public void playNext(Long roomId, UserAuthData authData) {
        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId)
            .orElse(new RoomPlaybackState(
                entityManager.getReference(ListeningRoom.class, roomId)
            ));
        RoomState room = findRoom(roomId, state.getCurrentQueueEntryId());
        playbackService.playNext(room, state.getCurrentQueueEntryId(), authData);
    }

    public void playPrev(Long roomId, UserAuthData authData) {
        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId)
            .orElse(new RoomPlaybackState(
                entityManager.getReference(ListeningRoom.class, roomId)
            ));
        RoomState room = findRoom(roomId, state.getCurrentQueueEntryId());
        playbackService.playPrev(room, state.getCurrentQueueEntryId(), authData);
    }
}
