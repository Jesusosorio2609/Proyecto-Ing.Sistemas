package is.generador.core.application.context;

import is.generador.core.application.registry.DiagnosticTracker;
import is.generador.core.application.registry.DiagnosticTrackerImpl;
import is.generador.core.application.registry.ExtractionSnapshot;
import is.generador.core.application.registry.ExtractionSnapshotImpl;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.registry.ResolutionRegistryImpl;
import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.classifier.UmlConcreteClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import is.generador.core.ports.DomainPolicyProvider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import is.generador.core.domain.spec.AggregationKind;

public final class ResolutionContext {

    private final ExtractionSnapshot extractionSnapshot;
    private ResolutionRegistry resolutionRegistry;
    private final DiagnosticTracker diagnosticTracker;
    private boolean frozen;

    public ResolutionContext(Map<String, UmlClassifier> initialClassifiers,
            DomainPolicyProvider policyProvider) {
        extractionSnapshot = new ExtractionSnapshotImpl(
                Objects.requireNonNull(initialClassifiers, "initialClassifiers"),
                Objects.requireNonNull(policyProvider, "policyProvider"));
        diagnosticTracker = new DiagnosticTrackerImpl();
        refreshResolutionRegistry();
    }

    public ResolutionContext freeze() {
        if (!frozen) {
            extractionSnapshot.freeze();
            refreshResolutionRegistry();
            frozen = true;
        }
        return this;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void registerClassifier(UmlClassifier classifier) {
        checkNotFrozen("register classifier");
        extractionSnapshot.registerClassifier(classifier);
        refreshResolutionRegistry();
    }

    public void registerExternalClassifier(String qualifiedName,
            UmlClassifier externalClassifier) {
        checkNotFrozen("register external classifier");
        extractionSnapshot.registerExternalClassifier(qualifiedName, externalClassifier);
        refreshResolutionRegistry();
    }

    public void replaceClassifier(String fqn, UmlConcreteClassifier newClassifier) {
        checkNotFrozen("replace classifier");
        extractionSnapshot.replaceClassifier(fqn, newClassifier);
        refreshResolutionRegistry();
    }

    public void registerStructuralRelationship(UmlRelationship relationship) {
        checkNotFrozen("register structural relationship");
        extractionSnapshot.registerStructuralRelationship(relationship);
        refreshResolutionRegistry();
    }

    public void registerUnresolvedType(String sourceClassifierFqn,
            String rawTypeName, List<String> contextImports) {
        checkNotFrozen("register unresolved type");
        extractionSnapshot.unresolvedTypes().registerType(
                sourceClassifierFqn, rawTypeName, contextImports);
    }

    public void registerUnresolvedAssociation(String sourceClassifierFqn,
            String rawTypeName, List<String> contextImports) {
        checkNotFrozen("register unresolved association");
        extractionSnapshot.unresolvedTypes().registerAssociation(
                sourceClassifierFqn, rawTypeName, contextImports);
    }

    public void registerUnresolvedHierarchy(String sourceClassifierFqn,
            String rawAncestorName, List<String> contextImports) {
        checkNotFrozen("register unresolved hierarchy");
        extractionSnapshot.unresolvedTypes().registerHierarchy(
                sourceClassifierFqn, rawAncestorName, contextImports);
    }

    public void registerUnresolvedAssociation(String sourceClassifierFqn,
            String rawTypeName, List<String> contextImports,
            AggregationKind kind, String multiplicity) {
        checkNotFrozen("register unresolved association");
        extractionSnapshot.unresolvedTypes().registerAssociation(
                sourceClassifierFqn, rawTypeName, contextImports, kind, multiplicity);
    }

    public void registerUnresolvedHierarchy(String sourceClassifierFqn,
            String rawAncestorName, List<String> contextImports,
            boolean isInterfaceImplementation) {
        checkNotFrozen("register unresolved hierarchy");
        extractionSnapshot.unresolvedTypes().registerHierarchy(
                sourceClassifierFqn, rawAncestorName, contextImports,
                isInterfaceImplementation);
    }

    public void registerUnresolvedNesting(
            String outerClassifierFqn,
            String innerClassifierFqn,
            boolean isStatic) {

        checkNotFrozen("register unresolved nesting");

        extractionSnapshot.unresolvedTypes().registerNesting(
                outerClassifierFqn,
                innerClassifierFqn,
                isStatic
        );
    }

    public void reportError(String filePath, String message, Exception cause) {
        diagnosticTracker.reportError(filePath, message, cause);
    }

    public void reportWarning(String filePath, String message) {
        diagnosticTracker.reportWarning(filePath, message);
    }

    public List<PendingResolution> getUnresolvedTypesLog() {
        return extractionSnapshot.getUnresolvedTypesLog();
    }

    public ExtractionSnapshot extractionSnapshot() {
        return extractionSnapshot;
    }

    public ResolutionRegistry resolutionRegistry() {
        return resolutionRegistry;
    }

    public DiagnosticTracker diagnosticTracker() {
        return diagnosticTracker;
    }

    private void checkNotFrozen(String action) {
        if (frozen) {
            throw new IllegalStateException("Cannot " + action + ": context is frozen");
        }
    }

    private void refreshResolutionRegistry() {
        resolutionRegistry = new ResolutionRegistryImpl(
                extractionSnapshot.availableClassifiers(),
                extractionSnapshot.structuralRelationships());
    }
}
