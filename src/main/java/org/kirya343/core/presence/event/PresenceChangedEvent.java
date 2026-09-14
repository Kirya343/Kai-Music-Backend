package org.kirya343.core.presence.event;

import org.kirya343.enums.UserPresence;

public record PresenceChangedEvent(
    String userOpenId,
    UserPresence presence
) {}
