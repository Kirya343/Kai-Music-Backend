package org.kirya343.features.playback.services.queue;

import java.util.List;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.datasource.repository.QueueItemRepository;
import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playback.dto.queue.QueueItemCreateDTO;
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
public class QueueCommandService {

    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addQueueList(List<QueueItemCreateDTO> list, UserAuthData authData) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        for (QueueItemCreateDTO dto : list) {

            addToQueue(room.getId(), authData.id(), dto);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(room.getId())
        );
    }
    
    @Transactional
    public void addToQueue(
        Long roomId,
        Long addedById,
        QueueItemCreateDTO dto
    ) {
        if (dto.position() == null) {
            queueItemRepository.insertQueueItemAtEnd(
                roomId,
                dto.audioId(),
                addedById
            );

            return;
        }

        int maxPosition = queueItemRepository.getMaxPosition(roomId);

        if (dto.position() < 0 || dto.position() > maxPosition + 1) {
            throw new IllegalArgumentException(
                "Position must be between 0 and " + (maxPosition + 1)
            );
        }

        queueItemRepository.shiftQueueItems(
            roomId,
            dto.position()
        );

        queueItemRepository.insertQueueItem(
            roomId,
            dto.audioId(),
            dto.position(),
            addedById
        );
    }

    @Transactional
    public void removeQueueList(List<Long> list, UserAuthData authData) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        for (Long queueItemId : list) {

            removeFromQueue(room.getId(), queueItemId, authData);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(room.getId())
        );
    }

    @Transactional 
    public void removeFromQueue(Long roomId, Long queueItemId, UserAuthData authData) {
        queueItemRepository.deleteByIdAndUserRoom(queueItemId, authData.id());

        log.debug("deleted");

        queueItemRepository.normalizePositions(roomId);

        log.debug("shifted");
    }
}
