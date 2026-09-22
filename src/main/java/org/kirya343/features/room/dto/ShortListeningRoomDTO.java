package org.kirya343.features.room.dto;

import org.kirya343.features.room.datasource.ListeningRoom;

public record ShortListeningRoomDTO(
    Long id,
    String title,
    Long ownerId,
    String code,
    Integer membersCount
) {

    public static ShortListeningRoomDTO ofRoom(ListeningRoom room) {
        return new ShortListeningRoomDTO(
            room.getId(), 
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room", 
            room.getOwner().getId(),
            room.getCode(),
            room.getMembers().size()
        );
    }
}
