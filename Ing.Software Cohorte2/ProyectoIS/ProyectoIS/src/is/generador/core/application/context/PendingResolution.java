package is.generador.core.application.context;

import java.util.List;
import java.util.Objects;

public record PendingResolution(String sourceClassifierFqn, String rawTypeName,
        List<String> contextImports, boolean isHierarchyClaim,
        boolean isNestingClaim, boolean isStaticNesting,
        boolean isInterfaceImplementation) {

    public PendingResolution {
        sourceClassifierFqn = requireText(sourceClassifierFqn, "sourceClassifierFqn");
        rawTypeName = requireText(rawTypeName, "rawTypeName");
        contextImports = List.copyOf(Objects.requireNonNull(contextImports, "contextImports"));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value;
    }
}
