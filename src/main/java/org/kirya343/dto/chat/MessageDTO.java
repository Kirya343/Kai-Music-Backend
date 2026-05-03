package org.kirya343.dto.chat;

import java.time.Instant;

public record MessageDTO(
    String openId,
    String content,
    Instant timestamp,
    String senderOpenId,
    String chatOpenId,
    boolean isRead
) {}