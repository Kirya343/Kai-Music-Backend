package org.kirya343.infrastructure.diagnostics.commands;

public interface DiagnosticCommand {

    String getName();

    String getDescription();

    Object execute(String[] args);
}