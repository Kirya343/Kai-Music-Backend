package org.kirya343.dto.chat;

public record ChatMemberDTO(
    String chatOpenId,
    String openId,
    String name,
    String avatarUrl
) {}