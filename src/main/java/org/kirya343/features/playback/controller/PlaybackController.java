package org.kirya343.features.playback.controller;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playback.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
public class PlaybackController {

    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    
    @GetMapping("/{roomId}/playback-state")
    public PlaybackStateDTO getPlaybackState(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return PlaybackStateDTO.ofState(state);
    }
}
