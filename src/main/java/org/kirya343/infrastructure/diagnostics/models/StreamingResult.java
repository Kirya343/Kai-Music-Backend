package org.kirya343.infrastructure.diagnostics.models;

import org.kirya343.infrastructure.diagnostics.core.StreamingDataProvider;

public record StreamingResult(
    StreamingDataProvider provider,
    StreamMode mode
) implements CommandResult {
}