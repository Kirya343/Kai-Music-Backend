package org.kirya343.features.playlist.service;

import java.util.List;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playlist.datasource.repository.QueueItemRepository;
import org.kirya343.features.playlist.dto.queue.QueueItemCreateDTO;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j 
@RequiredArgsConstructor 
public class PlaylistCommandService {

    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addQueueList(List<QueueItemCreateDTO> list, UserAuthData authData) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        for (QueueItemCreateDTO dto : list) {

            addToQueue(room.getPlaylist().getId(), authData.id(), dto);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(room.getId())
        );
    }
    
    @Transactional
    public void addToQueue(
        Long playlistId,
        Long addedById,
        QueueItemCreateDTO dto
    ) {
        if (dto.position() == null) {
            queueItemRepository.insertQueueItemAtEnd(
                playlistId,
                dto.audioId(),
                addedById
            );

            return;
        }

        int maxPosition = queueItemRepository.getMaxPosition(playlistId);

        if (dto.position() < 0 || dto.position() > maxPosition + 1) {
            throw new IllegalArgumentException(
                "Position must be between 0 and " + (maxPosition + 1)
            );
        }

        queueItemRepository.shiftQueueItems(
            playlistId,
            dto.position()
        );

        queueItemRepository.insertQueueItem(
            playlistId,
            dto.audioId(),
            dto.position(),
            addedById
        );
    }

    @Transactional
    public void removeQueueList(List<Long> list, UserAuthData authData) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        for (Long queueItemId : list) {

            removeFromQueue(room.getPlaylist().getId(), queueItemId, authData);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(room.getId())
        );
    }

    @Transactional 
    public void removeFromQueue(Long playlistId, Long queueItemId, UserAuthData authData) {
        queueItemRepository.deleteByIdAndUserRoom(queueItemId, authData.id());

        log.debug("deleted");

        queueItemRepository.normalizePositions(playlistId);

        log.debug("shifted");
    }
}
