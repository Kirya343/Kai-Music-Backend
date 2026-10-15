package org.kirya343.infrastructure.diagnostics.console;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;
import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticManager;
import org.kirya343.infrastructure.diagnostics.models.CommandResult;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DiagnosticTerminal {

    private final Terminal terminal;
    private final DiagnosticScreen screen;
    private final DiagnosticManager diagnosticManager;

    private Thread streamingThread;

    private final List<String> commandHistory = new ArrayList<>();

    private int historyIndex = 0;
    private String savedInput = "";

    public DiagnosticTerminal(
        Terminal terminal,
        DiagnosticScreen screen,
        DiagnosticManager diagnosticManager
    ) {
        this.terminal = terminal;
        this.screen = screen;
        this.diagnosticManager = diagnosticManager;
    }

    public void start() throws IOException {
        StringBuilder input = new StringBuilder();
        Attributes originalAttributes = terminal.enterRawMode();

        try {
            terminal.puts(InfoCmp.Capability.enter_ca_mode);
            terminal.flush();

            screen.render();

            while (true) {
                int key = terminal.reader().read(100);

                // EOF или Ctrl+C.
                if (key == -1 || key == 3) {
                    break;
                }

                // Тайм-аут чтения.
                if (key == -2) {
                    continue;
                }

                // ANSI escape-последовательности.
                if (key == 27) {
                    handleEscapeSequence(input);
                    continue;
                }

                // Enter.
                if (key == '\r' || key == '\n') {
                    String command = input.toString().trim();
                    input.setLength(0);

                    if (command.equalsIgnoreCase("exit")) {
                        break;
                    }

                    if (!command.isEmpty()) {
                        // Не сохраняем подряд одинаковые команды.
                        if (commandHistory.isEmpty()
                            || !commandHistory.get(
                                commandHistory.size() - 1
                            ).equals(command)) {
                            commandHistory.add(command);
                        }

                        historyIndex = commandHistory.size();
                        savedInput = "";

                        executeCommand(command);
                    }

                    screen.setInput("");
                    continue;
                }

                // Backspace.
                if (key == 127 || key == 8) {
                    if (!input.isEmpty()) {
                        input.deleteCharAt(input.length() - 1);
                    }
                } else if (key >= 32 && key <= 126) {
                    // Печатные ASCII-символы.
                    input.append((char) key);
                }

                screen.setInput(input.toString());
            }
        } finally {
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
            terminal.flush();
            terminal.setAttributes(originalAttributes);
            terminal.flush();
        }
    }

    /**
     * Читает ANSI escape-последовательность и передаёт её
     * обработчику клавиш.
     */
    private void handleEscapeSequence(StringBuilder input)
        throws IOException {

        int next = terminal.reader().read(100);

        // Одиночный Escape или неполная последовательность.
        if (next < 0 || next != '[') {
            return;
        }

        StringBuilder sequence = new StringBuilder("\033[");

        while (true) {
            int key = terminal.reader().read(100);

            if (key < 0) {
                return;
            }

            sequence.append((char) key);

            // Конечный символ ANSI-последовательности.
            if (key >= 0x40 && key <= 0x7E) {
                break;
            }
        }

        handleKeySequence(sequence.toString(), input);
    }

    /**
     * Обрабатывает историю команд и прокрутку содержимого.
     */
    private void handleKeySequence(
        String sequence,
        StringBuilder input
    ) {
        switch (sequence) {
            // Стрелка вверх: предыдущая команда.
            case "\033[A" -> {
                if (commandHistory.isEmpty()) {
                    return;
                }

                // Сохраняем ввод, существовавший до просмотра истории.
                if (historyIndex == commandHistory.size()) {
                    savedInput = input.toString();
                }

                if (historyIndex > 0) {
                    historyIndex--;

                    replaceInput(
                        input,
                        commandHistory.get(historyIndex)
                    );
                }
            }

            // Стрелка вниз: следующая команда.
            case "\033[B" -> {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;

                    replaceInput(
                        input,
                        commandHistory.get(historyIndex)
                    );
                } else if (historyIndex < commandHistory.size()) {
                    // Выходим из истории и восстанавливаем ввод.
                    historyIndex = commandHistory.size();

                    replaceInput(input, savedInput);
                }
            }

            // Page Up: прокрутка вверх на одну страницу.
            case "\033[5~" ->
                screen.scrollUp(screen.getContentLines());

            // Page Down: прокрутка вниз на одну страницу.
            case "\033[6~" ->
                screen.scrollDown(screen.getContentLines());

            default -> {
                // Неизвестные последовательности игнорируются.
            }
        }
    }

    /**
     * Заменяет одновременно буфер ввода и отображаемый текст.
     */
    private void replaceInput(
        StringBuilder input,
        String value
    ) {
        input.setLength(0);
        input.append(value);

        screen.setInput(value);
    }

    /**
     * Выполняет диагностическую команду.
     */
    private void executeCommand(String input) {
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
                streamingThread = new Thread(
                    () -> streamingResult.stream().start(content -> {
                        if (streamingThread == null
                            || Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        switch (streamingResult.mode()) {
                            case APPEND -> screen.addLines(
                                List.of(content.split("\\R"))
                            );
                            case REPLACE -> screen.setContent(
                                List.of(content.split("\\R"))
                            );
                        }
                    })
                );

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