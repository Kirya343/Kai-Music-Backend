package org.kirya343.dto.audio;

public record ShortListeningRoomDTO(
    Long id,
    String title,
    Long ownerId,
    Integer membersCount
) {
}
