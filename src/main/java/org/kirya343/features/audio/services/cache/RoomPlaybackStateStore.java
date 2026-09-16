package org.kirya343.features.audio.services.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class RoomPlaybackStateStore {

    private final Map<Long, RoomPlaybackState> rooms = new ConcurrentHashMap<>();

    public RoomPlaybackState get(Long roomId) {
        return rooms.get(roomId);
    }

    public RoomPlaybackState computeIfAbsent(Long id, Supplier<RoomPlaybackState> factory) {
        return rooms.computeIfAbsent(id, k -> factory.get());
    }

    public Set<Long> roomIds() {
        return rooms.keySet();
    }
}
