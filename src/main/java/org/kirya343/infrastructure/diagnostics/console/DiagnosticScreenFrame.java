package org.kirya343.infrastructure.diagnostics.console;

import java.util.List;

public record DiagnosticScreenFrame(
    String status,
    String input,
    List<String> content,
    int contentHeight
) {
    public DiagnosticScreenFrame {
        status = status == null ? "ONLINE" : status;
        input = input == null ? "" : input;
        content = List.copyOf(content);
        contentHeight = Math.max(1, contentHeight);
    }
}