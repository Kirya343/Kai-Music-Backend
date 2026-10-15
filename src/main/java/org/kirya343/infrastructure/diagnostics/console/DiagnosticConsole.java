package org.kirya343.infrastructure.diagnostics.console;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticConsole implements ApplicationRunner {

    private final DiagnosticTerminal diagnosticTerminal;

    public DiagnosticConsole(DiagnosticTerminal diagnosticTerminal) {
        this.diagnosticTerminal = diagnosticTerminal;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread consoleThread = new Thread(
            () -> {
                try {
                    System.err.println(
                        "[DIAGNOSTICS] Starting diagnostic terminal..."
                    );

                    diagnosticTerminal.start();
                } catch (Exception exception) {
                    System.err.println(
                        "[DIAGNOSTICS] Failed to start terminal"
                    );
                    exception.printStackTrace(System.err);
                }
            },
            "diagnostic-console"
        );

        consoleThread.setDaemon(true);
        consoleThread.start();
    }
}