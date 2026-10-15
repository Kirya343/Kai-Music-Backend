package org.kirya343.infrastructure.diagnostics.core;

import io.github.bucket4j.distributed.remote.CommandResult;

public interface DiagnosticComponent {

    String getDiagnosticId();

    CommandResult<?> getDiagnosticState();
}