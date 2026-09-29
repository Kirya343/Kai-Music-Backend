package org.kirya343.features.room.dto;

import java.util.List;

public record MainPageRequest(
    List<ShortRoomDTO> publicRooms,
    long activeListners,
    long activeRooms
) {
}
