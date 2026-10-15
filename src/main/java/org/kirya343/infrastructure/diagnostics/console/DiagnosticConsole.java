package org.kirya343.infrastructure.diagnostics.console;

import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
public class DiagnosticConsole implements ApplicationRunner {

    private final DiagnosticManager diagnosticManager;

    public DiagnosticConsole(DiagnosticManager diagnosticManager) {
        this.diagnosticManager = diagnosticManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread consoleThread = new Thread(
            this::start,
            "diagnostic-console"
        );

        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                executeCommand(input);
            }
        }
    }

    private void executeCommand(String input) {
        String[] parts = input.split("\\s+");

        String commandName = parts[0];

        String[] commandArgs = Arrays.copyOfRange(
            parts,
            1,
            parts.length
        );

        try {
            DiagnosticCommand command =
                diagnosticManager.findCommand(commandName);

            Object result = command.execute(commandArgs);

            System.out.println(result);
        } catch (Exception exception) {
            System.err.println(
                "Diagnostic error: " + exception.getMessage()
            );
        }
    }
}