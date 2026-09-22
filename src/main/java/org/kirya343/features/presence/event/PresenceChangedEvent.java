package org.kirya343.features.presence.event;

import org.kirya343.features.presence.UserPresence;

public record PresenceChangedEvent(
    String userOpenId,
    UserPresence presence
) {}
