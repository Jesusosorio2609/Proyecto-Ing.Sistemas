package is.generador.core.application.context;

import java.util.ArrayList;
import java.util.List;
import is.generador.core.domain.spec.AggregationKind;

public final class UnresolvedTypeRegistry {
    private final List<PendingResolution> resolutions = new ArrayList<>();

    public void registerType(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports) {
        resolutions.add(new PendingResolution(sourceClassifierFqn, rawTypeName,
                contextImports, ResolutionKind.TYPE_DEPENDENCY, false, false));
    }

    public void registerAssociation(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports) {
        registerAssociation(sourceClassifierFqn, rawTypeName, contextImports, AggregationKind.NONE, "1");
    }

    public void registerAssociation(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports, AggregationKind kind, String multiplicity) {
        resolutions.add(new PendingResolution(sourceClassifierFqn, rawTypeName,
                contextImports, ResolutionKind.ASSOCIATION, false, false, kind, multiplicity));
    }

    public void registerHierarchy(String sourceClassifierFqn, String rawAncestorName,
            List<String> contextImports) {
        registerHierarchy(sourceClassifierFqn, rawAncestorName, contextImports, false);
    }

    public void registerHierarchy(String sourceClassifierFqn, String rawAncestorName,
            List<String> contextImports, boolean isInterfaceImplementation) {
        resolutions.add(new PendingResolution(sourceClassifierFqn, rawAncestorName,
                contextImports, ResolutionKind.HIERARCHY, false,
                isInterfaceImplementation));
    }

    public void registerNesting(String outerClassifierFqn, String innerClassifierFqn,
            boolean isStatic) {
        resolutions.add(new PendingResolution(outerClassifierFqn, innerClassifierFqn,
                List.of(), ResolutionKind.NESTING, isStatic, false));
    }

    public List<PendingResolution> getResolutions() { return List.copyOf(resolutions); }
    public int size() { return resolutions.size(); }
    public List<PendingResolution> getLog() { return getResolutions(); }
}
