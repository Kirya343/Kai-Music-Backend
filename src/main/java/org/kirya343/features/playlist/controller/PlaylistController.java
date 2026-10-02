package org.kirya343.features.playlist.controller;

import java.util.List;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.dto.PlaylistCreateDTO;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.dto.queue.QueueItemCreateDTO;
import org.kirya343.features.playlist.service.PlaylistCommandService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/{playlistId}")
    public PlaylistDTO getPlaylist(
        @PathVariable Long playlistId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);

        return PlaylistDTO.ofPlaylist(playlist);
    }

    @GetMapping("/my")
    public List<PlaylistDTO> getMyPlaylist(
        @AuthenticationPrincipal UserAuthData authData
    ) {
        List<Playlist> playlists = playlistRepository.findUserPlaylists(authData.id());

        return PlaylistDTO.ofListShort(playlists);
    }

    @PostMapping
    public PlaylistDTO createPlaylist(
        @RequestBody PlaylistCreateDTO dto,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        return playlistCommandService.createPlaylist(dto, authData);
    }

    @PostMapping("/{playlistId}/import")
    public void importPlaylist(
        @PathVariable Long playlistId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistCommandService.importPlaylist(playlistId, authData);
    }

    @DeleteMapping("/{playlistId}")
    public void deletePlaylist(
        @PathVariable Long playlistId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistRepository.deleteById(playlistId);
    }

    @PatchMapping("/{playlistId}/mode")
    public void updatePlaybackMode(
        @PathVariable Long playlistId, 
        @RequestParam PlaybackMode mode
    ) {
        playlistCommandService.updatePlaybackMode(playlistId, mode);
    }

    @PostMapping("/{playlistId}/queue")
    public void addToQueue(
        @PathVariable Long playlistId,
        @RequestBody List<QueueItemCreateDTO> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistCommandService.addQueueListToRoom(list, authData);
    }

    @DeleteMapping("/{playlistId}/queue")
    public void removeFromQueue(
        @PathVariable Long playlistId,
        @RequestBody List<Long> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        playlistCommandService.removeQueueListFromRoom(list, authData);
    }
}
