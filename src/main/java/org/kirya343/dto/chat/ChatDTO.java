package org.kirya343.dto.chat;

import org.kirya343.enums.chat.ChatStatus;
import org.kirya343.enums.chat.ChatType;

public record ChatDTO(
    String openId,
    String name,
    long unreadCount,
    ChatStatus status,
    ChatType type,
    MessageDTO lastMessage
) {}
