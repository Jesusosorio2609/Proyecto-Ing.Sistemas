package is.generador.ui.model;

import is.generador.ProjectSummary;
import is.generador.core.domain.model.UmlModel;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record InventoryViewModel(Path sourceDirectory, ProjectSummary summary,
        UmlModel umlModel, String report, List<DiagnosticEntry> diagnostics,
        Set<String> whitelistedLibraries, Set<String> blacklistedLibraries,
        Set<String> excludedPackages, Set<String> excludedClasses,
        Map<String, ClassifierFileDetails> classifierDetails,
        LocalDateTime analyzedAt) {
    public InventoryViewModel {
        Objects.requireNonNull(sourceDirectory, "sourceDirectory");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(umlModel, "umlModel");
        Objects.requireNonNull(report, "report");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        whitelistedLibraries = Set.copyOf(Objects.requireNonNull(
                whitelistedLibraries, "whitelistedLibraries"));
        blacklistedLibraries = Set.copyOf(Objects.requireNonNull(
                blacklistedLibraries, "blacklistedLibraries"));
        excludedPackages = Set.copyOf(Objects.requireNonNull(
                excludedPackages, "excludedPackages"));
        excludedClasses = Set.copyOf(Objects.requireNonNull(
                excludedClasses, "excludedClasses"));
        classifierDetails = Map.copyOf(Objects.requireNonNull(
                classifierDetails, "classifierDetails"));
        Objects.requireNonNull(analyzedAt, "analyzedAt");
    }
}
