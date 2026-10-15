package org.kirya343.infrastructure.diagnostics.commands;

import org.kirya343.infrastructure.diagnostics.models.CommandResult;

public interface DiagnosticCommand {

    String getName();

    String getDescription();

    CommandResult execute(String[] args);
}