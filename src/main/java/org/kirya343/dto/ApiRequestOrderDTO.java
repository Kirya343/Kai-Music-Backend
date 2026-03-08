package org.kirya343.dto;

public record ApiRequestOrderDTO(
    int boostCount,
    String boostDuration,
    String discordServerLink
) {
}
