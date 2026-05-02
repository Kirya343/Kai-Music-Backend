package org.kirya343.core.audio.room;

import java.util.List;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.datasource.model.audio.RoomPlaybackState;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.datasource.repository.audio.RoomPlaybackStateRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;

    public List<Long> loadQueue(Long roomId) {

        return queueItemRepository.findByRoomIdOrderByPosition(roomId)
            .stream()
            .map(qi -> qi.getAudio().getId())
            .toList();

    }

    public QueueItem nextTrack(Long roomId) {
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        RoomPlaybackState playbackState = roomPlaybackStateRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        Long previousEntryId = playbackState.getCurrentQueueEntryId();

        QueueItem queueItem = null;

        switch (room.getPlaybackMode()) {
            case NORMAL:
                
                queueItem = queueItemRepository.findNextTrack(roomId, previousEntryId).orElse(null);

                break;

            case REPEAT_ALL:
                
                queueItem = queueItemRepository.findNextTrack(roomId, previousEntryId).orElse(null);

                if (queueItem == null) {
                    queueItem = queueItemRepository.findFirstByRoomIdOrderByPositionAsc(roomId).orElse(null);
                }

                break;

            case REPEAT_ONE:
                
                queueItem = queueItemRepository.findByRoomIdAndId(roomId, previousEntryId).orElse(null);

                break;

            case SHUFFLE:

                queueItem = queueItemRepository.findRandomTrack(roomId).orElse(null);
                
                break;
        }

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

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