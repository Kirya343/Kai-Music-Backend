package org.kirya343.dto.chat;

import java.util.List;

import org.kirya343.dto.auth.UserAuthData;

public record ChatsLoadedEvent(
    UserAuthData authData,
    List<ChatDTO> chats
) {}
