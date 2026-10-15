package org.kirya343.infrastructure.diagnostics.core;

import org.kirya343.infrastructure.diagnostics.models.DiagnosticResult;

public interface DiagnosticComponent {

    String getDiagnosticId();

    DiagnosticResult getDiagnosticState();
}