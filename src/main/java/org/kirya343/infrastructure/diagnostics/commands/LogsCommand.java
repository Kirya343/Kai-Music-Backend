package org.kirya343.infrastructure.diagnostics.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.kirya343.infrastructure.diagnostics.core.StreamingDataProvider;
import org.kirya343.infrastructure.diagnostics.models.CommandResult;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

@Component
public class LogsCommand implements DiagnosticCommand {

    private int limit = 50;
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
    public CommandResult execute(String[] args) {
        boolean follow = Arrays.asList(args).contains("-f");

        for (String arg : args) {
            if (!arg.equals("-f")) {
                limit = Integer.parseInt(arg);
                break;
            }
        }

        if (follow) {
            return new StreamingResult(
                createLogProvider(),
                StreamMode.APPEND
            );
        }

        try (Stream<String> lines = Files.lines(LOG_FILE)) {
            List<String> result = lines.toList();

            int start = Math.max(0, result.size() - limit);

            return new TextResult(
                String.join(
                    System.lineSeparator(),
                    result.subList(start, result.size())
                )
            );
        } catch (IOException exception) {
            return new TextResult(
                "Failed to read logs: " + exception.getMessage()
            );
        }
    }

    private StreamingDataProvider createLogProvider() {
        return new StreamingDataProvider() {
            private long position;
            private boolean initialized;

            @Override
            public synchronized String getData() {
                StringBuilder content = new StringBuilder();

                try (RandomAccessFile file = new RandomAccessFile(
                    LOG_FILE.toFile(),
                    "r"
                )) {
                    if (!initialized) {
                        List<String> lines = Files.readAllLines(LOG_FILE);
                        int start = Math.max(0, lines.size() - limit);

                        content.append(
                            String.join(
                                System.lineSeparator(),
                                lines.subList(start, lines.size())
                            )
                        );

                        position = file.length();
                        initialized = true;

                        return content.toString();
                    }

                    if (file.length() < position) {
                        position = 0;
                    }

                    file.seek(position);

                    String line;

                    while ((line = file.readLine()) != null) {
                        if (!content.isEmpty()) {
                            content.append(System.lineSeparator());
                        }

                        content.append(decodeLine(line));
                    }

                    position = file.getFilePointer();

                } catch (IOException exception) {
                    return "Failed to read logs: " + exception.getMessage();
                }

                return content.toString();
            }
        };
    }

    private String decodeLine(String line) {
        return new String(
            line.getBytes(StandardCharsets.ISO_8859_1),
            StandardCharsets.UTF_8
        );
    }
}