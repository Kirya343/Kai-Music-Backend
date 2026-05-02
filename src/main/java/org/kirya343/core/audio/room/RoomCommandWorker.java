package org.kirya343.core.audio.room;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.audio.RoomPlaybackEvent;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.commands.Next;
import org.kirya343.dto.room.commands.Pause;
import org.kirya343.dto.room.commands.Play;
import org.kirya343.dto.room.commands.Prev;
import org.kirya343.dto.room.commands.RoomCommand;
import org.kirya343.dto.room.commands.Tick;
import org.kirya343.dto.room.results.NoOp;
import org.kirya343.dto.room.results.Paused;
import org.kirya343.dto.room.results.PlaybackResult;
import org.kirya343.dto.room.results.Resumed;
import org.kirya343.dto.room.results.TrackChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomCommandWorker {

    private final ApplicationEventPublisher publisher;
    private final RoomStateStore rooms;
    private final PlaybackService playbackService;
    private final ExecutorService executor = Executors.newFixedThreadPool(16);
    private final Set<Long> startedRooms = ConcurrentHashMap.newKeySet();
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomWebSocketService webSocketService;
    private static final Logger logger = LoggerFactory.getLogger(RoomCommandWorker.class);

    public void start(RoomCommandQueue queue, Long roomId) {

        if (!startedRooms.add(roomId)) {
            return; // уже запущен
        }

        executor.submit(() -> {

            BlockingQueue<RoomCommand> q = queue.getQueue(roomId);

            while (true) {
                RoomCommand cmd = q.take();
                handle(cmd);
            }
        });
    }

    private void handle(RoomCommand cmd) {
        RoomState room = rooms.computeIfAbsent(
            cmd.roomId(),
            () -> createRoomState(cmd.roomId())
        );

        UserAuthData authData = null;

        PlaybackResult result = switch (cmd) {
            case Play c -> {
                logger.debug("Включаем трек");
                authData = c.user();
                yield playbackService.play(room, c);
            }
            case Pause c -> {
                logger.debug("Ставим трек на паузу");
                authData = c.user();
                yield playbackService.pause(room, c);
            }
            case Next c -> {
                logger.debug("Переключаем трек вперёд");
                authData = c.user();
                yield playbackService.next(room, c);
            }
            case Prev c -> {
                logger.debug("Переключаем трек назад");
                authData = c.user();
                yield playbackService.prev(room, c);
            }
            case Tick c -> playbackService.tick(room, c.now());
            default -> throw new RuntimeException("Введена неверная команда");
        };

        if (!(result instanceof NoOp)) {
            PlaybackStateDTO dto = mapToDto(result, room);

            publisher.publishEvent(
                new RoomPlaybackEvent(room.getRoomId(), dto.entryId(), dto.position(), dto.pause(), authData)
            );
    
            webSocketService.broadcastPlaybackState(room.getRoomId(), dto);
        }
    }

    private RoomState createRoomState(Long roomId) {

        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("комната не найдена"));

        return new RoomState(
            room.getId(),
            null,
            0,
            false
        );
    }

    private PlaybackStateDTO mapToDto(PlaybackResult result, RoomState room) {

        switch (result) {
            case Resumed r -> {
                return new PlaybackStateDTO(
                    r.user().name(), 
                    r.trackId(), 
                    r.position(), 
                    false
                );
            }
            case Paused r -> {
                return new PlaybackStateDTO(
                    r.user().name(), 
                    r.trackId(), 
                    r.position(), 
                    true
                );
            }
            case TrackChanged r -> {
                return new PlaybackStateDTO(
                    r.user().name(), 
                    r.trackId(), 
                    Long.valueOf(0), 
                    false
                );
            }
            default -> throw new RuntimeException("Введена неверная команда");
        }
    }
}
