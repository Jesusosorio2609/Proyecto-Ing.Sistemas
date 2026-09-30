package is.generador.core.application.context;

import java.util.List;
import java.util.Objects;
import is.generador.core.domain.spec.AggregationKind;

public record PendingResolution(String sourceClassifierFqn, String rawTypeName,
        List<String> contextImports, ResolutionKind kind,
        boolean isStaticNesting,
        boolean isInterfaceImplementation, AggregationKind aggregationKind,
        String targetMultiplicity) {

    public PendingResolution(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports, ResolutionKind kind,
            boolean isStaticNesting, boolean isInterfaceImplementation) {
        this(sourceClassifierFqn, rawTypeName, contextImports, kind,
                isStaticNesting, isInterfaceImplementation, AggregationKind.NONE, "1");
    }

    public PendingResolution {
        sourceClassifierFqn = requireText(sourceClassifierFqn, "sourceClassifierFqn");
        rawTypeName = requireText(rawTypeName, "rawTypeName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(aggregationKind, "aggregationKind");
        targetMultiplicity = requireText(targetMultiplicity, "targetMultiplicity");
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
