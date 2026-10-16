package org.kirya343.infrastructure.diagnostics.core;

import java.util.Arrays;
import java.util.List;

import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.console.DiagnosticScreen;
import org.kirya343.infrastructure.diagnostics.models.CommandResult;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class TerminalCommandExecutor {

    private final DiagnosticManager diagnosticManager;
    private final DiagnosticScreen screen;

    private Thread streamingThread;
    
    /**
     * Выполняет диагностическую команду.
     */
    public void executeCommand(String input) {
        stopStreaming();

        String trimmedInput = input.trim();

        if (trimmedInput.isEmpty()) {
            return;
        }

        String[] parts = trimmedInput.split("\\s+");
        String commandName = parts[0];

        String[] commandArgs = Arrays.copyOfRange(
            parts,
            1,
            parts.length
        );

        try {
            DiagnosticCommand command =
                diagnosticManager.findCommand(commandName);

            CommandResult result = command.execute(commandArgs);

            if (result instanceof StreamingResult streamingResult) {
                streamingThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (streamingThread == null) {
                            return;
                        }

                        String content = streamingResult.provider().getData();

                        switch (streamingResult.mode()) {
                            case APPEND -> screen.addLines(
                                List.of(content.split("\\R"))
                            );
                            case REPLACE -> screen.setContent(
                                List.of(content.split("\\R"))
                            );
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

                streamingThread.setDaemon(true);
                streamingThread.start();

                return;
            }

            if (result instanceof TextResult textResult) {
                screen.setContent(
                    List.of(textResult.content().split("\\R"))
                );

                return;
            }

            screen.setContent(
                List.of("Unsupported diagnostic result: " + result)
            );
        } catch (Exception exception) {
            screen.setContent(
                List.of("Diagnostic error: " + exception.getMessage())
            );
        }
    }

    private void stopStreaming() {
        if (streamingThread != null && streamingThread.isAlive()) {
            streamingThread.interrupt();
            streamingThread = null;
        }
    }
}
