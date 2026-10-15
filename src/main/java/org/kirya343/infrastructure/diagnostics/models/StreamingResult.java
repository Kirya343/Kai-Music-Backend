package org.kirya343.infrastructure.diagnostics.models;

import org.kirya343.infrastructure.diagnostics.core.DiagnosticStream;

public record StreamingResult(
    DiagnosticStream stream,
    StreamMode mode
) implements CommandResult {
}