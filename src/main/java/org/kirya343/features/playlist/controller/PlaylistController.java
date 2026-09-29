package org.kirya343.features.playlist.controller;

import java.util.List;
import java.util.Set;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.dto.queue.QueueItemCreateDTO;
import org.kirya343.features.playlist.service.PlaylistCommandService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.services.RoomQueryService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@Slf4j 
@RequiredArgsConstructor 
@RequestMapping("/playlist")
public class PlaylistController {

    private final PlaylistRepository playlistRepository;
    private final PlaylistCommandService playlistCommandService;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomQueryService roomQueryService;
    private final SimpMessagingTemplate messagingTemplate;

    @PatchMapping("/{playlistId}/mode")
    public void updatePlaybackMode(
        @PathVariable Long playlistId, 
        @RequestParam PlaybackMode mode
    ) {
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

    @PostMapping("/queue")
    public void addToQueue(
        @RequestBody List<QueueItemCreateDTO> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistCommandService.addQueueList(list, authData);
    }

    @DeleteMapping("/queue")
    public void removeFromQueue(
        @RequestBody List<Long> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistCommandService.removeQueueList(list, authData);
    }
}
