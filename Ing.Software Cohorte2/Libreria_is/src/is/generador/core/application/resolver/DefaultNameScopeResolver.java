package is.generador.core.application.resolver;

import is.generador.core.domain.classifier.UmlClassifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DefaultNameScopeResolver implements NameScopeResolver {

    @Override
    public Optional<String> resolveFqn(String rawTypeName,
            UmlClassifier sourceClassifier, List<String> contextImports,
            Map<String, UmlClassifier> availableClassifiers) {
        Objects.requireNonNull(rawTypeName, "rawTypeName");
        Objects.requireNonNull(sourceClassifier, "sourceClassifier");
        Objects.requireNonNull(contextImports, "contextImports");
        Objects.requireNonNull(availableClassifiers, "availableClassifiers");

        String rawName = normalize(rawTypeName);
        if (rawName.isEmpty()) {
            return Optional.empty();
        }
        if (availableClassifiers.containsKey(rawName)) {
            return Optional.of(rawName);
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(sourceClassifier.qualifiedName() + "." + rawName);
        if (!sourceClassifier.namespace().isRoot()) {
            candidates.add(sourceClassifier.namespace().toString() + "." + rawName);
        }

        String simpleName = simpleName(rawName);
        for (String importName : contextImports) {
            if (importName.endsWith(".*")) {
                candidates.add(importName.substring(0, importName.length() - 2)
                        + "." + rawName);
            } else if (simpleName(importName).equals(simpleName)) {
                candidates.add(importName);
            }
        }

        for (String candidate : candidates) {
            if (availableClassifiers.containsKey(candidate)) {
                return Optional.of(candidate);
            }
        }

        List<String> simpleMatches = new ArrayList<>();
        for (String qualifiedName : availableClassifiers.keySet()) {
            if (simpleName(qualifiedName).equals(simpleName)) {
                simpleMatches.add(qualifiedName);
            }
        }
        return simpleMatches.size() == 1
                ? Optional.of(simpleMatches.get(0)) : Optional.empty();
    }

    private String normalize(String rawTypeName) {
        String result = rawTypeName.trim();
        while (result.endsWith("[]")) {
            result = result.substring(0, result.length() - 2);
        }
        int genericStart = result.indexOf('<');
        return genericStart >= 0 ? result.substring(0, genericStart).trim() : result;
    }

    private String simpleName(String qualifiedName) {
        int separator = Math.max(qualifiedName.lastIndexOf('.'),
                qualifiedName.lastIndexOf('$'));
        return separator >= 0 ? qualifiedName.substring(separator + 1) : qualifiedName;
    }
}
