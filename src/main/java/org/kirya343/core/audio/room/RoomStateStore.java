package org.kirya343.core.audio.room;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class RoomStateStore {

    private final Map<Long, RoomState> rooms = new ConcurrentHashMap<>();

    public RoomState get(Long roomId) {
        return rooms.get(roomId);
    }

    public RoomState computeIfAbsent(Long id, Supplier<RoomState> factory) {
        return rooms.computeIfAbsent(id, k -> factory.get());
    }

    public Set<Long> roomIds() {
        return rooms.keySet();
    }
}
