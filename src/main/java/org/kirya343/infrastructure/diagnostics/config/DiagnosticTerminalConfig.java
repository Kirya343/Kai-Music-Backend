package org.kirya343.infrastructure.diagnostics.config;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class DiagnosticTerminalConfig {

    @Bean
    public Terminal terminal() throws IOException {
        return TerminalBuilder.builder()
            .system(true)
            .build();
    }
}