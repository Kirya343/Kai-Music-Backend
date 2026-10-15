package org.kirya343.infrastructure.diagnostics.commands;

import org.kirya343.infrastructure.diagnostics.models.DiagnosticResult;

public interface DiagnosticCommand {

    String getName();

    String getDescription();

    DiagnosticResult execute(String[] args);
}