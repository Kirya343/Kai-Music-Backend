package org.kirya343.core.presence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.enums.UserPresence;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class PresenceService {
    
    private final Map<String, UserPresence> presences = new ConcurrentHashMap<>();

    public void userConnected(String userOpenId) {
        updatePresence(userOpenId, UserPresence.ONLINE);
    }

    public void userDisconnected(String userOpenId) {
        updatePresence(userOpenId, UserPresence.OFFLINE);
    }

    public long countAll() {
        return presences.size();
    }

    public void updatePresence(String userOpenId, UserPresence presence) {

        UserPresence current = presences.get(userOpenId);

        if (current == UserPresence.ONLINE) {
            return;
        }

        presences.put(userOpenId, presence);

        log.debug("Пользователь {} теперь {}", userOpenId, presence.toString());
    }

    public UserPresence getPresence(String userOpenId) {
        return presences.getOrDefault(userOpenId, UserPresence.OFFLINE);
    }
}
