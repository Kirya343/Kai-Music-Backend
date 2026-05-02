package org.kirya343.core.audio.room;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.kirya343.dto.room.commands.RoomCommand;
import org.springframework.stereotype.Component;

@Component
public class RoomCommandQueue {

    private final Map<Long, BlockingQueue<RoomCommand>> queues = new ConcurrentHashMap<>();

    public void submit(RoomCommand cmd) {
        queues
            .computeIfAbsent(cmd.roomId(), id -> new LinkedBlockingQueue<>())
            .add(cmd);
    }

    public BlockingQueue<RoomCommand> getQueue(Long roomId) {
        return queues.computeIfAbsent(roomId, id -> new LinkedBlockingQueue<>());
    }
}
