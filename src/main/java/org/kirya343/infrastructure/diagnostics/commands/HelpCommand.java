package org.kirya343.infrastructure.diagnostics.commands;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.kirya343.infrastructure.diagnostics.models.CommandResult;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class HelpCommand implements DiagnosticCommand {

    private final List<DiagnosticCommand> commands;
    
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows all coommands";
    }

    @Override
    public CommandResult execute(String[] args) {
        String content = commands.stream()
            .sorted(Comparator.comparing(DiagnosticCommand::getName))
            .map(command -> String.format(
                "%-15s %s",
                command.getName(),
                command.getDescription()
            ))
            .collect(Collectors.joining("\n"));

        return new TextResult(content);
    }
}
