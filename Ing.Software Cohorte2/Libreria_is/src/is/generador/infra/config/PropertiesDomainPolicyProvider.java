package is.generador.infra.config;

import is.generador.core.ports.DomainPolicyProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public final class PropertiesDomainPolicyProvider implements DomainPolicyProvider {
    private static final Set<String> NATIVE_TYPES = Set.of(
            "byte", "short", "int", "long", "float", "double",
            "boolean", "char", "void");
    private final Set<String> whitelist;
    private final Set<String> blacklist;

    public PropertiesDomainPolicyProvider() {
        this(Set.of(), Set.of());
    }

    public PropertiesDomainPolicyProvider(Set<String> whitelist,
            Set<String> blacklist) {
        this.whitelist = Set.copyOf(Objects.requireNonNull(whitelist, "whitelist"));
        this.blacklist = Set.copyOf(Objects.requireNonNull(blacklist, "blacklist"));
    }

    public static PropertiesDomainPolicyProvider from(Path configurationFile)
            throws IOException {
        if (configurationFile == null || !Files.isRegularFile(configurationFile)) {
            return new PropertiesDomainPolicyProvider();
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(configurationFile)) {
            properties.load(reader);
        }
        return new PropertiesDomainPolicyProvider(
                valuesOf(properties.getProperty("libraries.whitelist", "")),
                valuesOf(properties.getProperty("libraries.blacklist", "")));
    }

    @Override public boolean isAllowedExternalType(String fqn) {
        return !isBlacklisted(fqn) && matches(fqn, whitelist);
    }
    @Override public boolean isLanguageNativeType(String fqn) {
        return NATIVE_TYPES.contains(fqn);
    }
    @Override public boolean isStructural(String fqn) {
        return !isBlacklisted(fqn);
    }
    @Override public boolean isBlacklisted(String fqn) {
        return matches(fqn, blacklist);
    }
    @Override public boolean isStandardLibrary(String fqn) {
        return fqn.startsWith("java.") || fqn.startsWith("javax.");
    }

    private static Set<String> valuesOf(String text) {
        Set<String> values = new LinkedHashSet<>();
        for (String value : text.split(",")) {
            if (!value.isBlank()) values.add(value.trim());
        }
        return values;
    }

    private static boolean matches(String fqn, Set<String> values) {
        return values.stream().anyMatch(value -> fqn.equals(value)
                || fqn.startsWith(value + ".")
                || fqn.endsWith("." + value));
    }
}
