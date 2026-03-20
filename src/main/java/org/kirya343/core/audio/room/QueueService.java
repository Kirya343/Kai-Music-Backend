package org.kirya343.core.audio.room;

import java.util.List;

import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;

    public List<Long> loadQueue(Long roomId) {

        return queueItemRepository.findByRoomIdOrderByPosition(roomId)
            .stream()
            .map(qi -> qi.getAudio().getId())
            .toList();

    }

    public AudioFile nextTrack(Long roomId, Long previousAudioId) {
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        QueueItem queueItem = null;

        switch (room.getPlaybackMode()) {
            case NORMAL:
                
                queueItem = queueItemRepository.findNextTrack(roomId, previousAudioId).orElse(null);

                break;

            case REPEAT_ALL:
                
                queueItem = queueItemRepository.findNextTrack(roomId, previousAudioId).orElse(null);

                if (queueItem == null) {
                    queueItem = queueItemRepository.findFirstByRoomIdOrderByPositionAsc(roomId).orElse(null);
                }

                break;

            case REPEAT_ONE:
                
                queueItem = queueItemRepository.findByRoomIdAndAudioId(roomId, previousAudioId).orElse(null);

                break;

            case SHUFFLE:

                queueItem = queueItemRepository.findRandomTrack(roomId).orElse(null);
                
                break;
        }

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

        return queueItem.getAudio();
    }
}