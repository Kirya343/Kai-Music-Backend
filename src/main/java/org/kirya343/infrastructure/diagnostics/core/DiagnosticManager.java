package org.kirya343.infrastructure.diagnostics.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticManager {

    private final Map<String, DiagnosticCommand> commands =
        new ConcurrentHashMap<>();

    public DiagnosticManager(List<DiagnosticCommand> commands) {
        commands.forEach(this::registerCommand);
    }

    public void registerCommand(DiagnosticCommand command) {
        DiagnosticCommand existing = commands.putIfAbsent(
            command.getName(),
            command
        );

        if (existing != null) {
            throw new IllegalArgumentException(
                "Command already registered: " + command.getName()
            );
        }
    }

    public DiagnosticCommand findCommand(String name) {
        DiagnosticCommand command = commands.get(name);

        if (command == null) {
            throw new IllegalArgumentException(
                "Unknown command: " + name
            );
        }

        return command;
    }
}