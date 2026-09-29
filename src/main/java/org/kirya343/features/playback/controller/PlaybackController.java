package org.kirya343.features.playback.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playback.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.ChangeTrack;
import org.kirya343.features.playback.dto.commands.RoomCommand;
import org.kirya343.features.playback.dto.commands.UpdatePlayback;
import org.kirya343.features.playback.services.command.PlaybackCommandWorker;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@RequestMapping("/playback")
public class PlaybackController {

    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final PlaybackCommandWorker roomCommandWorker;
    private final Map<String, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long UPDATE_DELAY_MS = 300;
    
    @GetMapping("/{roomId}/playback-state")
    public PlaybackStateDTO getPlaybackState(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return PlaybackStateDTO.ofState(state);
    }
    
    @PostMapping("/{roomId}/update-state")
    public void updatePlaybackState(
        @RequestBody PlaybackStateDTO state,
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (shouldIgnore(authData.id(), roomId, "updatePlayback")) {
            return;
        }

        RoomCommand cmd = new UpdatePlayback(roomId, state, authData);
        
        roomCommandWorker.submit(cmd);
    }

    @PostMapping("/{roomId}/change-track")
    public void changeTrack(
        @RequestParam String changing,
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (shouldIgnore(authData.id(), roomId, "changeTrack")) {
            return;
        }

        RoomCommand cmd = new ChangeTrack(roomId, changing, authData);
        
        roomCommandWorker.submit(cmd);
    }

    private boolean shouldIgnore(Long userId, Long roomId, String action) {
        long now = System.currentTimeMillis();

        String key = userId + ":" + roomId + ":" + action;

        Long last = lastUpdate.get(key);

        if (last != null && now - last < UPDATE_DELAY_MS) {
            return true;
        }

        lastUpdate.put(key, now);
        return false;
    }
}
