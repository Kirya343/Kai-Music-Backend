package org.kirya343.features.playlist.controller;

import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PatchMapping("/{playlistId}/mode")
    public void updatePlaybackMode(@PathVariable Long playlistId, @RequestParam PlaybackMode mode) {
        playlistRepository.updatePlaybackMode(playlistId, mode);
    }
}
