package is.generador.ui.service;

import is.generador.SoyLaPuertaJava;
import is.generador.ui.model.InventoryViewModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class InventoryUiService {
    public static final String AUTOMATIC_EXCLUSION = "com.universidad.AppGenerator";
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
        excludedClasses.add(AUTOMATIC_EXCLUSION);
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

    public void saveExclusions(Path sourceDirectory, Set<String> excludedPackages,
            Set<String> excludedClasses) throws IOException {
        Objects.requireNonNull(sourceDirectory, "sourceDirectory");
        Objects.requireNonNull(excludedPackages, "excludedPackages");
        Objects.requireNonNull(excludedClasses, "excludedClasses");
        Path configurationFile = sourceDirectory.toAbsolutePath().normalize()
                .getParent().resolve("analisis.properties");
        Properties properties = loadProperties(configurationFile);
        updateProperty(properties, "exclude.packages", excludedPackages);
        Set<String> configurableClasses = new LinkedHashSet<>(excludedClasses);
        configurableClasses.remove(AUTOMATIC_EXCLUSION);
        updateProperty(properties, "exclude.classes", configurableClasses);

        Path temporaryFile = Files.createTempFile(configurationFile.getParent(),
                "analisis-", ".properties.tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporaryFile)) {
                properties.store(writer, "Project analysis configuration");
            }
            try {
                Files.move(temporaryFile, configurationFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, configurationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void updateProperty(Properties properties, String key,
            Set<String> values) {
        String normalized = values.stream().map(String::trim)
                .filter(value -> !value.isEmpty()).distinct().sorted()
                .reduce((a, b) -> a + "," + b).orElse("");
        if (normalized.isEmpty()) {
            properties.remove(key);
        } else {
            properties.setProperty(key, normalized);
        }
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
