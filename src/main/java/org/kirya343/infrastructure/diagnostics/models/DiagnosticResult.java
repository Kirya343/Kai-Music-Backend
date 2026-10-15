package org.kirya343.infrastructure.diagnostics.models;

import java.util.Map;

public record DiagnosticResult(
    String componentId,
    Map<String, Object> state
) {}
