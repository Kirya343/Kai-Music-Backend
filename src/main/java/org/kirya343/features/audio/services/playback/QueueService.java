package org.kirya343.features.audio.services.playback;

import java.util.List;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.model.QueueItem;
import org.kirya343.features.audio.datasource.model.RoomPlaybackState;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.audio.datasource.repository.QueueItemRepository;
import org.kirya343.features.audio.datasource.repository.RoomPlaybackStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    public List<Long> loadQueue(Long roomId) {

        return queueItemRepository.findByRoomIdOrderByPosition(roomId)
            .stream()
            .map(qi -> qi.getAudio().getId())
            .toList();

    }

    public QueueItem nextTrack(Long roomId) {
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        RoomPlaybackState playbackState = roomPlaybackStateRepository.findById(roomId).orElseThrow();

        Long previousEntryId = playbackState.getCurrentQueueEntryId();

        logger.debug("Переключаем трек в комнате {}, номер предыдущего трека: {}", roomId, previousEntryId);

        QueueItem queueItem = switch (room.getPlaybackMode()) {
            case NORMAL ->
                queueItemRepository
                    .findNextTrack(roomId, previousEntryId)
                    .orElse(null);

            case REPEAT_ALL -> {
                QueueItem next = queueItemRepository
                    .findNextTrack(roomId, previousEntryId)
                    .orElse(null);

                if (next == null) {
                    next = queueItemRepository
                        .findFirstByRoomIdOrderByPositionAsc(roomId)
                        .orElse(null);
                }

                yield next;
            }

            case REPEAT_ONE ->
                queueItemRepository
                    .findByRoomIdAndId(roomId, previousEntryId)
                    .orElse(null);

            case SHUFFLE ->
                queueItemRepository
                    .findRandomTrack(roomId)
                    .orElse(null);
            
        };

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

        logger.debug("Режим проигрывания комнаты: {}", room.getPlaybackMode());
        logger.debug("Id следующего трека: {}", queueItem.getId());

        return queueItem;
    }

    public QueueItem prevTrack(Long roomId) {

        RoomPlaybackState playbackState = roomPlaybackStateRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        QueueItem queueItem = queueItemRepository.findPrevTrack(roomId, playbackState.getCurrentQueueEntryId()).orElse(null);

        if (queueItem == null) {
            queueItem = queueItemRepository.findFirstByRoomIdOrderByPositionDesc(roomId).orElse(null);
        }

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

        return queueItem;
    }
}