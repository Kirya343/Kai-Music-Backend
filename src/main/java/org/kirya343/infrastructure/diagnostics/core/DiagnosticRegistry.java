package org.kirya343.infrastructure.diagnostics.core;

import java.util.Collection;
import java.util.Optional;

public interface DiagnosticRegistry {

    void register(DiagnosticComponent component);

    Optional<DiagnosticComponent> find(String diagnosticId);

    Collection<DiagnosticComponent> findAll();
}
