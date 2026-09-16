package org.kirya343.features.audio.services.playback;

import org.kirya343.features.audio.services.AudioService;
import org.kirya343.features.audio.services.cache.RoomPlaybackState;
import org.kirya343.features.audio.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.audio.datasource.model.QueueItem;
import org.kirya343.features.audio.datasource.repository.QueueItemRepository;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.room.dto.commands.Pause;
import org.kirya343.features.room.dto.commands.Play;
import org.kirya343.features.room.dto.commands.Prev;
import org.kirya343.features.room.dto.results.Paused;
import org.kirya343.features.room.dto.results.PlaybackResult;
import org.kirya343.features.room.dto.results.Resumed;
import org.kirya343.features.room.dto.results.TrackChanged;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class PlaybackService {

    private final QueueService queueService;
    private final AudioService audioService;
    private final QueueItemRepository queueItemRepository;
    private final AudioStreamWorkerManager audioStreamWorkerManager;

    public PlaybackResult play(RoomPlaybackState room, Play cmd) {

        PlaybackStateDTO state = cmd.state();
        long pos = room.getPosition(System.nanoTime());

        if (room.getDuration() == 0) {
            QueueItem entry = queueItemRepository.findByRoomIdAndId(
                    room.getRoomId(), 
                    state.entryId()
                )
                .orElseThrow();

            Long durationFromDB = entry.getAudio().getDuration();
            long duration = durationFromDB != null 
                    ? durationFromDB 
                    : audioService.getDuration(entry.getAudio().getPath());

            room.setDuration(duration);
        }

        room.setCurrentQueueEntryId(state.entryId());
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(state.position());
        room.setPaused(false);

        log.debug(
            "\n\nВозобновляем проигрывание песни: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
            state.entryId(), room.getRoomId(), cmd.user().name());

        log.debug(
            "Следующая песня через: {} сек", 
            room.getDuration() - room.getLastPosition() - pos);

        log.debug(
            "Длина песни: {} сек, последняя позиция на: {} сек, текущая позиция на: {} сек", 
            room.getDuration(), room.getLastPosition(), pos);

        audioStreamWorkerManager.getWorker(room.getRoomId()).start(
            new PlaybackStateDTO(
                cmd.user().name(), 
                state.entryId(), 
                state.position(), 
                false)
        );

        return new Resumed(room.getRoomId(), state.entryId(), state.position(), cmd.user());
    }

    public PlaybackResult pause(RoomPlaybackState room, Pause cmd) {
        room.setPaused(true);

        PlaybackStateDTO state = cmd.state();

        log.debug(
            "\n\nСтавим на паузу песню: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
            state.entryId(), room.getRoomId(), cmd.user().name());

        audioStreamWorkerManager.getWorker(room.getRoomId()).stop();

        return new Paused(room.getRoomId(), state.entryId(), state.position(), cmd.user());
    }

    public PlaybackResult next(RoomPlaybackState room, Next cmd) {
        QueueItem entry = queueService.nextTrack(room.getRoomId());

        Long durationFromDB = entry.getAudio().getDuration();
        long duration = durationFromDB != null ? durationFromDB : audioService.getDuration(entry.getAudio().getPath());

        room.setCurrentQueueEntryId(entry.getId());
        room.setDuration(duration);
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(0);
        room.setPaused(false);

        audioStreamWorkerManager.getWorker(room.getRoomId()).switchTrack(
            new PlaybackStateDTO(
                cmd.user().name(), 
                entry.getAudio().getId(), 
                Long.valueOf(0), 
                false
            ));

        return new TrackChanged(room.getRoomId(), entry.getId(), cmd.user());
    }

    public PlaybackResult prev(RoomPlaybackState room, Prev cmd) {
        QueueItem entry = queueService.prevTrack(room.getRoomId());

        Long durationFromDB = entry.getAudio().getDuration();
        long duration = durationFromDB != null ? durationFromDB : audioService.getDuration(entry.getAudio().getPath());

        room.setCurrentQueueEntryId(entry.getId());
        room.setDuration(duration);
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(0);
        room.setPaused(false);

        audioStreamWorkerManager.getWorker(room.getRoomId()).switchTrack(
            new PlaybackStateDTO(
                cmd.user().name(), 
                entry.getAudio().getId(), 
                Long.valueOf(0), 
                false
            ));

        return new TrackChanged(room.getRoomId(), entry.getId(), cmd.user());
    }

    public boolean tick(RoomPlaybackState room, long now) {
        if (room.isPaused()) return false;
        
        long pos = room.getPosition(now);

        //log.debug("Тикаем комнату: {}, переключим? {}", room.getRoomId(), pos >= (room.getDuration() - room.getLastPosition()));

        if (pos >= (room.getDuration() - room.getLastPosition())) {
            log.debug("Отправляем команду на переключение следующей песни");

            return true;
        }

        return false;
    }
}
