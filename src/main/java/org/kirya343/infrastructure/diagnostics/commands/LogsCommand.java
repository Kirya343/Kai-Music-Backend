package org.kirya343.infrastructure.diagnostics.commands;

import org.kirya343.infrastructure.diagnostics.core.DiagnosticStream;
import org.kirya343.infrastructure.diagnostics.models.CommandResult;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
    public CommandResult execute(String[] args) {
        boolean follow = Arrays.asList(args).contains("-f");

        if (follow) {
            DiagnosticStream stream = this::streamLogs;

            return new StreamingResult(
                stream,
                StreamMode.APPEND
            );
        }

        int limit = 50;

        for (String arg : args) {
            if (!arg.equals("-f")) {
                limit = Integer.parseInt(arg);
                break;
            }
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

    private void streamLogs(java.util.function.Consumer<String> output) {
        try (RandomAccessFile file = new RandomAccessFile(
            LOG_FILE.toFile(),
            "r"
        )) {
            String line;

            while ((line = file.readLine()) != null) {
                output.accept(decodeLine(line));
            }

            while (!Thread.currentThread().isInterrupted()) {
                line = file.readLine();

                if (line != null) {
                    output.accept(decodeLine(line));
                    continue;
                }

                if (file.length() < file.getFilePointer()) {
                    file.seek(0);
                }

                Thread.sleep(500);
            }
        } catch (IOException exception) {
            output.accept(
                "Failed to read logs: " + exception.getMessage()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String decodeLine(String line) {
        return new String(
            line.getBytes(StandardCharsets.ISO_8859_1),
            StandardCharsets.UTF_8
        );
    }
}