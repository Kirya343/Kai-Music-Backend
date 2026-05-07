package org.kirya343.core.audio.room;

import org.kirya343.core.audio.AudioService;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.room.commands.Next;
import org.kirya343.dto.room.commands.Pause;
import org.kirya343.dto.room.commands.Play;
import org.kirya343.dto.room.commands.Prev;
import org.kirya343.dto.room.results.Paused;
import org.kirya343.dto.room.results.PlaybackResult;
import org.kirya343.dto.room.results.Resumed;
import org.kirya343.dto.room.results.TrackChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaybackService {

    private final QueueService queueService;
    private final AudioService audioService;
    private final QueueItemRepository queueItemRepository;
    private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

    public PlaybackResult play(RoomState room, Play cmd) {

        PlaybackStateDTO state = cmd.state();
        long pos = room.getPosition(System.nanoTime());

        if (room.getDuration() == 0) {
            QueueItem entry = queueItemRepository.findByRoomIdAndId(room.getRoomId(), state.entryId()).orElseThrow(
                () -> new EntityNotFoundException("Такой элемент очереди не найден"));

            Long durationFromDB = entry.getAudio().getDuration();
            long duration = durationFromDB != null ? durationFromDB : audioService.getDuration(entry.getAudio().getPath());
            room.setDuration(duration);
        }

        room.setCurrentQueueEntryId(state.entryId());
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(state.position());
        room.setPaused(false);

        logger.debug(
            "\n\nВозобновляем проигрывание песни: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
            state.entryId(), room.getRoomId(), cmd.user().name());

        logger.debug(
            "Следующая песня через: {} сек", 
            room.getDuration() - room.getLastPosition() - pos);

        logger.debug(
            "Длина песни: {} сек, последняя позиция на: {} сек, текущая позиция на: {} сек", 
            room.getDuration(), room.getLastPosition(), pos);

        return new Resumed(room.getRoomId(), state.entryId(), state.position(), cmd.user());
    }

    public PlaybackResult pause(RoomState room, Pause cmd) {
        room.setPaused(true);

        PlaybackStateDTO state = cmd.state();

        logger.debug(
            "\n\nСтавим на паузу песню: {} \nВ комнате: {} \nИнициировано пользователем: {}\n", 
            state.entryId(), room.getRoomId(), cmd.user().name());
        return new Paused(room.getRoomId(), state.entryId(), state.position(), cmd.user());
    }

    public PlaybackResult next(RoomState room, Next cmd) {
        QueueItem entry = queueService.nextTrack(room.getRoomId());

        Long durationFromDB = entry.getAudio().getDuration();
        long duration = durationFromDB != null ? durationFromDB : audioService.getDuration(entry.getAudio().getPath());

        room.setCurrentQueueEntryId(entry.getId());
        room.setDuration(duration);
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(0);
        room.setPaused(false);

        return new TrackChanged(room.getRoomId(), entry.getId(), cmd.user());
    }

    public PlaybackResult prev(RoomState room, Prev cmd) {
        QueueItem entry = queueService.prevTrack(room.getRoomId());

        Long durationFromDB = entry.getAudio().getDuration();
        long duration = durationFromDB != null ? durationFromDB : audioService.getDuration(entry.getAudio().getPath());

        room.setCurrentQueueEntryId(entry.getId());
        room.setDuration(duration);
        room.setResumedAt(System.nanoTime());
        room.setLastPosition(0);
        room.setPaused(false);

        return new TrackChanged(room.getRoomId(), entry.getId(), cmd.user());
    }

    public boolean tick(RoomState room, long now) {
        if (room.isPaused()) return false;
        
        long pos = room.getPosition(now);

        logger.debug("Тикаем комнату: {}, переключим? {}", room.getRoomId(), pos >= (room.getDuration() - room.getLastPosition()));

        if (pos >= (room.getDuration() - room.getLastPosition())) {
            logger.debug("Отправляем команду на переключение следующей песни");

            return true;
        }

        return false;
    }
}
