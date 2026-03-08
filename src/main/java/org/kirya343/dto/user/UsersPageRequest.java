package org.kirya343.dto.user;

import java.util.List;

public record UsersPageRequest(
    List<UserDTO> users,
    int totalPages
) {
}
