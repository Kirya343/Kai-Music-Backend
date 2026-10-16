package org.kirya343.infrastructure.diagnostics.core;

import org.kirya343.infrastructure.diagnostics.models.CommandResult;

public interface DiagnosticComponent {

    String getDiagnosticId();

    String[] getLastActions();

    DiagnosticTextBuilder getData();

    CommandResult getDiagnosticState();
}