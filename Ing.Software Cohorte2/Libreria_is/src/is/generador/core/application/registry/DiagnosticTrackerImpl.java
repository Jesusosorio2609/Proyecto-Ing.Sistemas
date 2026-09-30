package is.generador.core.application.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DiagnosticTrackerImpl implements DiagnosticTracker {
    private final List<String> diagnostics = new ArrayList<>();

    @Override
    public void reportError(String filePath, String message, Exception cause) {
        Objects.requireNonNull(cause, "cause");
        diagnostics.add("ERROR [" + filePath + "] " + message + ": " + cause.getMessage());
    }

    @Override
    public void reportWarning(String filePath, String message) {
        diagnostics.add("WARNING [" + filePath + "] " + message);
    }

    @Override public List<String> getReport() { return List.copyOf(diagnostics); }
}
