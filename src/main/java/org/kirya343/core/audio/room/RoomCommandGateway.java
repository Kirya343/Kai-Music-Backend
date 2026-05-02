package org.kirya343.core.audio.room;

import org.kirya343.dto.room.commands.RoomCommand;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomCommandGateway {

    private final RoomCommandQueue queue;
    private final RoomCommandWorker worker;

    public void submit(RoomCommand cmd) {
        Long roomId = cmd.roomId();

        worker.start(queue, roomId); // <-- запуск воркера (idempotent)

        queue.submit(cmd);
    }
}