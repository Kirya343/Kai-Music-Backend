package org.kirya343.features.playback.services.streaming;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.ChangeTrack;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.presence.UserPresence;
import org.kirya343.features.presence.services.PresenceService;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticComponent;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticTextBuilder;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.springframework.context.ApplicationEventPublisher;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoomStreamWorker implements DiagnosticComponent {

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final UserAudioStreamWorkerManager userAudioStreamWorkerManager;
    private final PresenceService presenceService;

    @Getter 
    private List<String> logs = new ArrayList<>();

    private final RoomPlaybackContext roomContext;

    private final Long roomId;

    private static final int CHUNK_INTERVAL_SECONDS = 2;

    private PlaybackStateDTO currentState;
    private ScheduledFuture<?> task;

    public RoomStreamWorker(
        Long roomId,
        ApplicationEventPublisher eventPublisher,
        UserAudioStreamWorkerManager userAudioStreamWorkerManager,
        PlaybackStateDTO currentState,
        RoomPlaybackContext roomContext,
        PresenceService presenceService
    ) {

        this.roomId = roomId;
        this.eventPublisher = eventPublisher;
        this.roomContext = roomContext;
        this.presenceService = presenceService;
        this.userAudioStreamWorkerManager = userAudioStreamWorkerManager;

        addLog("Created RoomStreamWorker for %s, with state: %s, %s, %s",
            roomId, currentState.entryId(), currentState.position(), currentState.pause());

        this.applyState(currentState);
    }

    public void applyState(PlaybackStateDTO state) {

        if (state == null) return;

        addLog("applyState %s", state.position());

        currentState = state;

        roomContext.getListeners().forEach(user -> {
            if (presenceService.getPresence(user) == UserPresence.ONLINE) {
                userAudioStreamWorkerManager.getWorker(user, state).applyState(state);
            }
        });

        addLog("START STATE: entryId=%s, position=%s", state.entryId(), state.position());

        if (task == null || task.isCancelled()) {
            task = scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        streamTick();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                0,
                CHUNK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        }
    }

    public void streamTick() {

        Set<String> listeners = roomContext.getListeners();

        addLog("Room tick %s, %s", roomId, listeners);

        /**
         * if current playback position is after than audio duration - cancel task and send Next command
         */
        if (currentState.position() > roomContext.getCurrentAudio().getDuration()) {

            addLog("Sending NEXT event: room=%s, becouse position(%s) is after audioDuration(%s)", 
                roomId,
                currentState.position(),
                roomContext.getCurrentAudio().getDuration()
            );

            eventPublisher.publishEvent(new ChangeTrack(roomId, "next", UserAuthData.server()));
            return;
        }

        try {
            listeners.forEach(user -> {
                try {
                    if (presenceService.getPresence(user) == UserPresence.ONLINE) {
                        userAudioStreamWorkerManager.getWorker(user, currentState).tick();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!currentState.pause()) {
            currentState = new PlaybackStateDTO(
                currentState.user(), 
                currentState.entryId(), 
                currentState.position() + CHUNK_INTERVAL_SECONDS, 
                currentState.pause()
            );
        }
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }

    // Diadnostic voids

    public String getDiagnosticId() {
        return "room-stream-worker-" + roomId;
    }

    public void addLog(String format, Object... args) {

        String newLog = String.format(format, args);

        log.debug(newLog);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String timestamp = LocalDateTime.now().format(formatter);

        logs.addLast(String.format("%s > %s", timestamp, newLog));

        if (logs.size() > 15) {
            logs.removeFirst();
        }
    }

    public DiagnosticTextBuilder getData() {

        DiagnosticTextBuilder builder = new DiagnosticTextBuilder("Stream worker of " + roomId);

        DiagnosticTextBuilder stateSection = new DiagnosticTextBuilder("Playback state: ")
            .addRow("position: " + currentState.position())
            .addRow("entryId: " + currentState.entryId())
            .addRow("pause: " + currentState.pause())
            .addRow("user: " + currentState.user());

        DiagnosticTextBuilder logsSection = new DiagnosticTextBuilder("Logs: ");

        for (String log : logs) {
            logsSection.addRow(log);
        }

        builder.addSection(stateSection);
        builder.addSection(logsSection);

        return builder;
    }

    public StreamingResult getDiagnosticState() {
        return new StreamingResult(
            () -> getData().getContent(),
            StreamMode.REPLACE
        );
    }
}