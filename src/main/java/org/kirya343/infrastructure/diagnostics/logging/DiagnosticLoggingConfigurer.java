package org.kirya343.infrastructure.diagnostics.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticLoggingConfigurer {

    private final LogBuffer logBuffer;

    public DiagnosticLoggingConfigurer(LogBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    @PostConstruct
    public void configure() {
        if (!(LoggerFactory.getILoggerFactory()
                instanceof LoggerContext context)) {
            return;
        }

        Logger rootLogger = context.getLogger(
            org.slf4j.Logger.ROOT_LOGGER_NAME
        );

        DiagnosticLogAppender appender =
            new DiagnosticLogAppender(logBuffer);

        appender.setContext(context);
        appender.setName("DIAGNOSTICS");
        appender.start();

        rootLogger.addAppender(appender);
    }
}