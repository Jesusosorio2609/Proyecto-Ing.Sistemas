package is.generador.core.application.context;

import java.util.List;
import java.util.Objects;

public record PendingResolution(String sourceClassifierFqn, String rawTypeName,
        List<String> contextImports, ResolutionKind kind,
        boolean isStaticNesting,
        boolean isInterfaceImplementation) {

    public PendingResolution {
        sourceClassifierFqn = requireText(sourceClassifierFqn, "sourceClassifierFqn");
        rawTypeName = requireText(rawTypeName, "rawTypeName");
        Objects.requireNonNull(kind, "kind");
        contextImports = Objects.requireNonNull(
                contextImports,
                "contextImports"
        ).stream()
                .map(importName -> requireText(importName, "contextImport"))
                .distinct()
                .toList();
    }

    public boolean isHierarchyClaim() {
        return kind == ResolutionKind.HIERARCHY;
    }

    public boolean isNestingClaim() {
        return kind == ResolutionKind.NESTING;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }
}
