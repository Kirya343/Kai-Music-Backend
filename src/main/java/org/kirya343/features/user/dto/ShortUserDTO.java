package org.kirya343.features.user.dto;

import java.util.Collection;
import java.util.List;

import org.kirya343.features.user.datasource.User;

public record ShortUserDTO(
    String openId,
    String name,
    String avatarUrl
) {

    public static ShortUserDTO ofUser(User user) {
        return new ShortUserDTO(user.getOpenId(), user.getName(), user.getAvatarUrl());
    }

    public static List<ShortUserDTO> ofList(Collection<User> users) {
        return users.stream().map(u -> ShortUserDTO.ofUser(u)).toList();
    }
}
