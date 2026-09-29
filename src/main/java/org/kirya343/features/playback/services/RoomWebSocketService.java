package org.kirya343.features.playback.services;

import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.room.dto.ShortRoomDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class RoomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPlaybackState(String userOpenId, PlaybackStateDTO state) {

        log.debug("Sending state info to user {}, pos {}, qi {}", userOpenId, state.position(), state.entryId());

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/playback",
            state
        );
    }

    public void broadcastRoomInfo(String userOpenId, ShortRoomDTO dto) {

        log.debug("Sending room({}) info to user {}", dto.id(), userOpenId);

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/room",
            dto
        );
    }

    public void broadcastAudioInfo(String userOpenId, AudioDTO dto) {

        log.debug("Sending audio({}) info to user {}", dto.id(), userOpenId);

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/audio-info",
            dto
        );
    }

    public void broadcastPlaylist(String userOpenId, PlaylistDTO playlist) {
        messagingTemplate.convertAndSendToUser(
            userOpenId, 
            "/queue/playlist", 
            playlist
        );
    }
}