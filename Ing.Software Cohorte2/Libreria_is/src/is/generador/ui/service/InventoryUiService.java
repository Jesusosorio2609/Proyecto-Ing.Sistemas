package is.generador.ui.service;

import is.generador.SoyLaPuertaJava;
import is.generador.ui.model.InventoryViewModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class InventoryUiService {
    private final SoyLaPuertaJava gateway;

    public InventoryUiService(SoyLaPuertaJava gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public InventoryViewModel analyze(Path selectedDirectory) throws IOException {
        Path sourceDirectory = resolveSourceDirectory(selectedDirectory);
        Path configurationFile = sourceDirectory.getParent()
                .resolve("analisis.properties");
        Properties policies = loadProperties(configurationFile);
        Set<String> whitelist = valuesOf(
                policies.getProperty("libraries.whitelist", ""));
        Set<String> blacklist = valuesOf(
                policies.getProperty("libraries.blacklist", ""));
        Set<String> excludedPackages = valuesOf(
                policies.getProperty("exclude.packages", ""));
        Set<String> excludedClasses = valuesOf(
                policies.getProperty("exclude.classes", ""));
        excludedClasses.add("com.universidad.AppGenerator");
        return new InventoryViewModel(
                sourceDirectory,
                gateway.analyzeProject(sourceDirectory, excludedPackages,
                        excludedClasses),
                gateway.analyzeModel(sourceDirectory,
                        configurationFile,
                        excludedPackages, excludedClasses),
                gateway.generarReporteDetallado(
                        new String[]{sourceDirectory.toString()}),
                List.of(),
                whitelist,
                blacklist,
                excludedPackages,
                excludedClasses,
                new SourceDetailExtractor().extract(sourceDirectory, blacklist),
                LocalDateTime.now());
    }

    private Properties loadProperties(Path configurationFile) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(configurationFile)) {
            try (var reader = Files.newBufferedReader(configurationFile)) {
                properties.load(reader);
            }
        }
        return properties;
    }

    private Set<String> valuesOf(String text) {
        Set<String> values = new LinkedHashSet<>();
        for (String value : text.split(",")) {
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values;
    }

    public Path resolveSourceDirectory(Path selectedDirectory) throws IOException {
        Path normalized = Objects.requireNonNull(
                selectedDirectory, "selectedDirectory").toAbsolutePath().normalize();
        Path sourceCandidate = normalized.resolve("src");
        if (Files.isDirectory(sourceCandidate)) {
            return sourceCandidate;
        }
        if (Files.isDirectory(normalized)) {
            return normalized;
        }
        throw new IOException("Source directory does not exist: " + normalized);
    }

    public static Path sourceFrom(String[] arguments) {
        for (String argument : arguments) {
            if (!argument.startsWith("--")) {
                return Path.of(argument);
            }
        }
        return Path.of("src");
    }
}
