package org.kirya343.dto.chat;

import java.util.List;

import org.kirya343.dto.user.ShortUserDTO;

public record ChatDetails(
    String chatOpenId,
    List<ShortUserDTO> interlocutors
) {}
