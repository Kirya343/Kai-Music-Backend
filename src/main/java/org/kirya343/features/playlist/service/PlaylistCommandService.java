package org.kirya343.features.playlist.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.model.QueueItem;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.datasource.repository.QueueItemRepository;
import org.kirya343.features.playlist.dto.PlaylistCreateDTO;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.dto.queue.QueueItemCreateDTO;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.services.RoomQueryService;
import org.kirya343.features.user.datasource.User;
import org.kirya343.infrastructure.security.services.UserAuthDataService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j 
@RequiredArgsConstructor 
public class PlaylistCommandService {

    private final QueueItemRepository queueItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserAuthDataService userAuthDataService;
    private final PlaylistRepository playlistRepository;
    private final RoomQueryService roomQueryService;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final SimpMessagingTemplate messagingTemplate;

    public PlaylistDTO createPlaylist(PlaylistCreateDTO dto, UserAuthData authData) {
        User user = userAuthDataService.parse(authData);

        if (user == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Playlist playlist = new Playlist(dto.title(), user);

        Playlist saved = playlistRepository.save(playlist);

        if (dto.audios() != null && dto.audios().size() > 0) {
            addQueueList(dto.audios(), saved.getId(), authData);
        }

        return PlaylistDTO.ofPlaylistShort(saved);
    }

    public void updatePlaybackMode(Long playlistId, PlaybackMode mode) {
        playlistRepository.updatePlaybackMode(playlistId, mode);

        ListeningRoom room = roomQueryService.findByPlaylistId(playlistId);

        if (room != null) {
            Set<String> listners = roomPlaybackContextStore.get(room.getId()).getListeners();

            for (String user : listners) {
                messagingTemplate.convertAndSendToUser(
                    user, 
                    "/queue/playback-mode", 
                    mode
                );
            }
        }
    }

    @Transactional
    public void importPlaylist(Long targetPlaylistId, Long playlistId, UserAuthData authData) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow();

        List<QueueItemCreateDTO> list = new ArrayList<>();

        for (QueueItem qi : playlist.getQueue()) {
            list.add(new QueueItemCreateDTO(qi.getAudio().getId(), qi.getPosition()));
        }

        if (list.size() > 0) {
            addQueueList(list, targetPlaylistId, authData);
        }
    }

    @Transactional
    public void addQueueList(List<QueueItemCreateDTO> list, Long playlistId, UserAuthData authData) {
        for (QueueItemCreateDTO dto : list) {
            addToQueue(playlistId, authData.id(), dto);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(playlistId)
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
    public void removeQueueList(List<Long> list, Long playlistId, UserAuthData authData) {

        for (Long queueItemId : list) {

            removeFromQueue(playlistId, queueItemId, authData);
        }

        eventPublisher.publishEvent(
            new QueueChangedEvent(playlistId)
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
