package is.generador.core.application.registry;

import java.util.List;

public interface DiagnosticTracker {
    void reportError(String filePath, String message, Exception cause);
    void reportWarning(String filePath, String message);
    List<String> getReport();
}
