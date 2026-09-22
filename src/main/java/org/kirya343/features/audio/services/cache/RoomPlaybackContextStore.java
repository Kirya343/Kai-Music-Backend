package org.kirya343.features.audio.services.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.datasource.repository.AudioFileRepository;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.audio.dto.event.RoomContextCreatedEvent;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class RoomPlaybackContextStore {

    private final RoomWebSocketService roomWebSocketService;
    private final Map<Long, RoomPlaybackContext> rooms = new ConcurrentHashMap<>();
    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioFileRepository audioFileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RoomPlaybackContext get(Long roomId) {
        return rooms.get(roomId);
    }

    public RoomPlaybackContext computeIfAbsent(Long roomId) {
        RoomPlaybackContext existing = rooms.get(roomId);

        if (existing != null) {
            return existing;
        }

        RoomPlaybackContext created = createRoomPlaybackState(roomId);
        RoomPlaybackContext context = rooms.putIfAbsent(roomId, created);

        if (context == null) {
            eventPublisher.publishEvent(new RoomContextCreatedEvent(roomId));
            return created;
        }

        return context;
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

    public void reloadAndResendContext(ListeningRoom room) {
        rooms.get(room.getId()).setRoom(room);

        RoomPlaybackContext context = rooms.get(room.getId());

        for (String user : rooms.get(room.getId()).getListeners()) {
             roomWebSocketService.broadcastRoomInfo(user, RoomDTO.ofRoomPlaybackContext(context));
        }
    }

    public void updateRoomAudio(Long roomId, PlaybackStateDTO state) {

        AudioFile audioFile = audioFileRepository
            .findAudioByQueueItem(state.entryId())
            .orElseThrow();

        this.computeIfAbsent(roomId).setCurrentAudio(audioFile);
    }
}
