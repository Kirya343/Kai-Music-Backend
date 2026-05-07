package org.kirya343.core.audio.room;

import java.util.concurrent.ThreadPoolExecutor;

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
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomWebSocketService webSocketService;
    private final RoomExecutorRegistry executorRegistry;
    private static final Logger logger = LoggerFactory.getLogger(RoomCommandWorker.class);

    public void submit(RoomCommand cmd) {

        ThreadPoolExecutor executor = executorRegistry.get(cmd.roomId());

        logExecutorsState();    

        executor.submit(() -> handle(cmd));
    }

    private void handle(RoomCommand cmd) {
        RoomState room = rooms.computeIfAbsent(
            cmd.roomId(),
            () -> createRoomState(cmd.roomId())
        );

        UserAuthData authData = null;

        PlaybackResult result = switch (cmd) {
            case Play c -> {
                PlaybackStateDTO state = c.state();
                logger.debug(
                    "\n\nВключаем трек: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    state.entryId(), room.getRoomId(), c.user().name()
                );

                authData = c.user();
                yield playbackService.play(room, c);
            }
            case Pause c -> {
                PlaybackStateDTO state = c.state();
                logger.debug(
                    "\n\nСтавим трек на паузу: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    state.entryId(), room.getRoomId(), c.user().name()
                );

                authData = c.user();
                yield playbackService.pause(room, c);
            }
            case Next c -> {
                logger.debug(
                    "\n\nПереключаем трек вперёд \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    room.getRoomId(), c.user().name()
                );
                authData = c.user();
                yield playbackService.next(room, c);
            }
            case Prev c -> {
                logger.debug(
                    "\n\nПереключаем трек назад \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    room.getRoomId(), c.user().name()
                );
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

    private void logExecutorsState() {

        executorRegistry.getAll().forEach((roomId, executor) -> {

            int queueSize = executor.getQueue().size();
            int active = executor.getActiveCount();
            int poolSize = executor.getPoolSize();

            logger.debug(
                "Room {} | queue={} | active={} | pool={}",
                roomId, queueSize, active, poolSize
            );
        });
    }
}
