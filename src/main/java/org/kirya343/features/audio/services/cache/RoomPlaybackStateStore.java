package org.kirya343.features.audio.services.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class RoomPlaybackStateStore {

    private final Map<Long, RoomPlaybackContext> rooms = new ConcurrentHashMap<>();
    private final ListeningRoomRepository listeningRoomRepository;

    public RoomPlaybackContext get(Long roomId) {
        return rooms.get(roomId);
    }

    public RoomPlaybackContext computeIfAbsent(Long id) {
        return rooms.computeIfAbsent(
            id,
            this::createRoomPlaybackState
        );
    }

    public Set<Long> roomIds() {
        return rooms.keySet();
    }

    private RoomPlaybackContext createRoomPlaybackState(Long roomId) {

        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("комната не найдена"));

        return new RoomPlaybackContext(
            room.getId(),
            null,
            0,
            false
        );
    }
}
