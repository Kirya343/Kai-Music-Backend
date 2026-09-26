package org.kirya343.features.playback.services;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.model.QueueItem;
import org.kirya343.features.playlist.datasource.repository.QueueItemRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    public QueueItem nextTrack(Long roomId) {
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        Playlist playlist = room.getPlaylist();

        RoomPlaybackState playbackState = room.getPlaybackState();

        Long previousEntryId = playbackState.getCurrentQueueEntryId();

        logger.debug("Переключаем трек в комнате {}, номер предыдущего трека: {}", roomId, previousEntryId);

        QueueItem queueItem = switch (playlist.getPlaybackMode()) {
            case NORMAL ->
                queueItemRepository
                    .findNextTrack(playlist.getId(), previousEntryId)
                    .orElse(null);

            case REPEAT_ALL -> {
                QueueItem next = queueItemRepository
                    .findNextTrack(playlist.getId(), previousEntryId)
                    .orElse(null);

                if (next == null) {
                    next = queueItemRepository
                        .findFirstByPlaylistIdOrderByPositionAsc(playlist.getId())
                        .orElse(null);
                }

                yield next;
            }

            case REPEAT_ONE ->
                queueItemRepository
                    .findByPlaylistIdAndId(playlist.getId(), previousEntryId)
                    .orElse(null);

            case SHUFFLE ->
                queueItemRepository
                    .findRandomTrack(playlist.getId())
                    .orElse(null);
            
        };

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

        logger.debug("Режим проигрывания комнаты: {}", playlist.getPlaybackMode());
        logger.debug("Id следующего трека: {}", queueItem.getId());

        return queueItem;
    }

    public QueueItem prevTrack(Long roomId) {
        
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        RoomPlaybackState playbackState = room.getPlaybackState();
        Playlist playlist = room.getPlaylist();

        QueueItem queueItem = queueItemRepository.findPrevTrack(playlist.getId(), playbackState.getCurrentQueueEntryId()).orElse(null);

        if (queueItem == null) {
            queueItem = queueItemRepository.findFirstByPlaylistIdOrderByPositionDesc(playlist.getId()).orElse(null);
        }

        if (queueItem == null) {
            throw new EntityNotFoundException("Нет подходящего трека для воспроизведения");
        } 

        return queueItem;
    }
}