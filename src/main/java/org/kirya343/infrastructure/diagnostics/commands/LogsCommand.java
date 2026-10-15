package org.kirya343.infrastructure.diagnostics.commands;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class LogsCommand implements DiagnosticCommand {

    private static final Path LOG_FILE = Path.of(
        "logs/application.log"
    );

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public String getDescription() {
        return "Shows application logs";
    }

    @Override
    public Object execute(String[] args) {
        int limit = 50;

        if (args.length > 0) {
            limit = Integer.parseInt(args[0]);
        }

        try (Stream<String> lines = Files.lines(LOG_FILE)) {
            List<String> result = lines
                .toList();

            int start = Math.max(0, result.size() - limit);

            return String.join(
                System.lineSeparator(),
                result.subList(start, result.size())
            );
        } catch (IOException exception) {
            return "Failed to read logs: " + exception.getMessage();
        }
    }
}