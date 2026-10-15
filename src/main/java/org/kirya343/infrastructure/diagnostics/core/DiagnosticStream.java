package org.kirya343.infrastructure.diagnostics.core;

import java.util.function.Consumer;

@FunctionalInterface
public interface DiagnosticStream {

    void start(Consumer<String> output);
}