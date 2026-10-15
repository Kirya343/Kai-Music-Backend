package org.kirya343.infrastructure.diagnostics.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class DiagnosticLogAppender extends AppenderBase<ILoggingEvent> {

    private final LogBuffer logBuffer;

    public DiagnosticLogAppender(LogBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = String.format(
            "%s [%s] %-5s %s - %s",
            new java.text.SimpleDateFormat("HH:mm:ss yyyy-MM-dd")
                .format(new java.util.Date(event.getTimeStamp())),
            event.getThreadName(),
            event.getLevel(),
            event.getLoggerName(),
            event.getFormattedMessage()
        );

        logBuffer.add(message);
    }
}