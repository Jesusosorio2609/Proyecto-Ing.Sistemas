package is.generador.ui.model;

import java.util.Objects;

public record DiagnosticEntry(Severity severity, String source, String message) {
    public DiagnosticEntry {
        Objects.requireNonNull(severity, "severity");
        source = Objects.requireNonNull(source, "source").trim();
        message = Objects.requireNonNull(message, "message").trim();
    }

    public enum Severity {
        ERROR, WARNING, INFO
    }
}
