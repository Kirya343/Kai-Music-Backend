package org.kirya343.infrastructure.diagnostics.core;

import java.util.List;

import org.kirya343.infrastructure.diagnostics.models.CommandResult;

public interface DiagnosticComponent {

    String getDiagnosticId();

    List<String> getLogs();

    DiagnosticTextBuilder getData();

    CommandResult getDiagnosticState();
}