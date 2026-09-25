package org.kirya343.features.playback.services.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.datasource.AudioFileRepository;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.services.RoomWebSocketService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class RoomPlaybackContextStore {

    private final RoomWebSocketService roomWebSocketService;
    private final Map<Long, RoomPlaybackContext> rooms = new ConcurrentHashMap<>();
    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioFileRepository audioFileRepository;

    public RoomPlaybackContext get(Long roomId) {
        return rooms.get(roomId);
    }

    public RoomPlaybackContext computeIfAbsent(Long roomId) {

        return rooms.computeIfAbsent(
            roomId,
            this::createRoomPlaybackState
        );
    }

    public Set<Long> roomIds() {
        return rooms.keySet();
    }

    private RoomPlaybackContext createRoomPlaybackState(Long roomId) {

        ListeningRoom room = listeningRoomRepository.findFullRoomById(roomId).orElseThrow();

        AudioFile audioFile = audioFileRepository.findCurrentAudioByRoom(roomId).orElse(null);

        return new RoomPlaybackContext(
            room.getId(),
            audioFile,
            room
        );
    }

    @Transactional 
    public void reloadAndResendContext(Long roomId) {
        ListeningRoom room = listeningRoomRepository
            .findById(roomId)
            .orElseThrow();

        rooms.get(room.getId()).setRoom(RoomDTO.ofRoom(room));

        RoomPlaybackContext context = rooms.get(room.getId());

        for (String user : rooms.get(room.getId()).getListeners()) {
            roomWebSocketService.broadcastRoomInfo(user, context.getFullRoom());
        }
    }

    public void updateRoomAudio(Long roomId, PlaybackStateDTO state) {

        AudioFile audioFile = audioFileRepository
            .findAudioByQueueItem(state.entryId())
            .orElse(null);

        if (audioFile != null) {
            this.computeIfAbsent(roomId).setCurrentAudio(audioFile);
        }   
    }
}
