package org.kirya343.infrastructure.diagnostics.models;

public record TextResult(
    String content
) implements CommandResult {

}
