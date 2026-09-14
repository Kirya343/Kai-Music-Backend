package org.kirya343.dto.room;

import java.util.List;

public record MainPageRequest(
    List<ShortListeningRoomDTO> publicRooms,
    long activeListners,
    long activeRooms
) {
}
