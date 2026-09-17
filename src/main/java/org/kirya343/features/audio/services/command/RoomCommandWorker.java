package org.kirya343.features.audio.services.command;

import java.util.concurrent.ThreadPoolExecutor;

import org.kirya343.features.audio.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.services.cache.RoomPlaybackStateStore;
import org.kirya343.features.audio.services.playback.PlaybackService;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.audio.dto.RoomPlaybackEvent;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.room.dto.commands.Pause;
import org.kirya343.features.room.dto.commands.Play;
import org.kirya343.features.room.dto.commands.Prev;
import org.kirya343.features.room.dto.commands.RoomCommand;
import org.kirya343.features.room.dto.commands.Tick;
import org.kirya343.features.room.dto.results.NoOp;
import org.kirya343.features.room.dto.results.Paused;
import org.kirya343.features.room.dto.results.PlaybackResult;
import org.kirya343.features.room.dto.results.Resumed;
import org.kirya343.features.room.dto.results.TrackChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class RoomCommandWorker {

    private final ApplicationEventPublisher publisher;
    private final RoomPlaybackStateStore rooms;
    private final PlaybackService playbackService;
    private final RoomWebSocketService webSocketService;
    private final RoomExecutorRegistry executorRegistry;

    public void submit(RoomCommand cmd) {

        ThreadPoolExecutor executor = executorRegistry.get(cmd.roomId());

        if (cmd.getClass() != Tick.class) {
            log.debug("Submit command {}", cmd.getClass());
        }
        
        executor.submit(() -> handle(cmd));
    }

    private void handle(RoomCommand cmd) {
        RoomPlaybackContext room = rooms.computeIfAbsent(cmd.roomId());

        UserAuthData authData = null;

        PlaybackResult result = switch (cmd) {
            case Play c -> {
                PlaybackStateDTO state = c.state();
                log.debug(
                    "\n\nВключаем трек: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    state.entryId(), room.getRoomId(), c.user().name()
                );

                authData = c.user();
                yield playbackService.play(room, c);
            }
            case Pause c -> {
                PlaybackStateDTO state = c.state();
                log.debug(
                    "\n\nСтавим трек на паузу: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    state.entryId(), room.getRoomId(), c.user().name()
                );

                authData = c.user();
                yield playbackService.pause(room, c);
            }
            case Next c -> {
                log.debug(
                    "\n\nПереключаем трек вперёд \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    room.getRoomId(), c.user().name()
                );
                authData = c.user();
                yield playbackService.next(room, c);
            }
            case Prev c -> {
                log.debug(
                    "\n\nПереключаем трек назад \nВ комнате: {} \nИнициировано пользователем: {}\n", 
                    room.getRoomId(), c.user().name()
                );
                authData = c.user();

                yield playbackService.prev(room, c);
            }
            case Tick c -> {
                //logger.debug("Тикаем комнату {}", room.getRoomId());

                // boolean turnOnNext = playbackService.tick(room, c.now());
                // if (turnOnNext) {
                //     RoomCommand command = new Next(
                //         room.getRoomId(), 
                //         new UserAuthData(
                //             Long.valueOf(0), 
                //             "", 
                //             "Server", 
                //             UserStatus.ACTIVE
                //         )
                //     );
                //     submit(command);
                // }
                yield new NoOp();
            }
            default -> throw new RuntimeException("Введена неверная команда");
        };

        if (!(result instanceof NoOp)) {
            PlaybackStateDTO stateDto = mapToDto(result, room);

            publisher.publishEvent(
                new RoomPlaybackEvent(
                    room.getRoomId(), 
                    stateDto.entryId(), 
                    stateDto.position(), 
                    stateDto.pause(), 
                    authData
                ));

            for (String user : room.getListeners()) {
                webSocketService.broadcastPlaybackState(user, stateDto);
            }
        }
    }

    private PlaybackStateDTO mapToDto(PlaybackResult result, RoomPlaybackContext room) {

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
