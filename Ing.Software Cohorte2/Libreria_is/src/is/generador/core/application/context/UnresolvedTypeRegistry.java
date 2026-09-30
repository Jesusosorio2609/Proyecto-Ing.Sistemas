package is.generador.core.application.context;

import java.util.ArrayList;
import java.util.List;

public final class UnresolvedTypeRegistry {
    private final List<PendingResolution> resolutions = new ArrayList<>();

    public void registerType(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports) {
        resolutions.add(new PendingResolution(sourceClassifierFqn, rawTypeName,
                contextImports, ResolutionKind.TYPE_DEPENDENCY, false, false));
    }

    public void registerAssociation(String sourceClassifierFqn, String rawTypeName,
            List<String> contextImports) {
        resolutions.add(new PendingResolution(sourceClassifierFqn, rawTypeName,
                contextImports, ResolutionKind.ASSOCIATION, false, false));
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
