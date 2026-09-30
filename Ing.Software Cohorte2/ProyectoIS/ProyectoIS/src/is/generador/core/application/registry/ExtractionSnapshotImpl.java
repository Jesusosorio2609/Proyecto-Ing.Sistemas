package is.generador.core.application.registry;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.context.UnresolvedTypeRegistry;
import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.classifier.UmlConcreteClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import is.generador.core.ports.DomainPolicyProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ExtractionSnapshotImpl implements ExtractionSnapshot {
    private final Map<String, UmlClassifier> classifiers;
    private final UnresolvedTypeRegistry unresolvedTypeRegistry = new UnresolvedTypeRegistry();
    private final DomainPolicyProvider policyProvider;
    private boolean frozen;
    private final List<UmlRelationship> structuralRelationships = new ArrayList<>();

    public ExtractionSnapshotImpl(Map<String, UmlClassifier> initialClassifiers,
            DomainPolicyProvider policyProvider) {
        this.classifiers = new LinkedHashMap<>(Objects.requireNonNull(initialClassifiers, "initialClassifiers"));
        this.policyProvider = Objects.requireNonNull(policyProvider, "policyProvider");
    }

    @Override public void freeze() { frozen = true; }
    @Override public Map<String, UmlClassifier> availableClassifiers() { return Map.copyOf(classifiers); }
    @Override public Optional<UmlClassifier> findClassifier(String qualifiedName) {
        return Optional.ofNullable(classifiers.get(Objects.requireNonNull(qualifiedName, "qualifiedName")));
    }

    @Override public void registerClassifier(UmlClassifier classifier) {
        checkNotFrozen();
        Objects.requireNonNull(classifier, "classifier");
        classifiers.put(classifier.qualifiedName(), classifier);
    }

    @Override public void replaceClassifier(String fqn, UmlConcreteClassifier newClassifier) {
        checkNotFrozen();
        Objects.requireNonNull(fqn, "fqn");
        Objects.requireNonNull(newClassifier, "newClassifier");
        if (!classifiers.containsKey(fqn)) throw new IllegalArgumentException("Unknown classifier: " + fqn);
        classifiers.put(fqn, newClassifier);
    }

    @Override public void registerExternalClassifier(String qualifiedName, UmlClassifier externalClassifier) {
        checkNotFrozen();
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(externalClassifier, "externalClassifier");
        boolean allowed = policyProvider.isAllowedExternalType(qualifiedName)
                || policyProvider.isLanguageNativeType(qualifiedName)
                || policyProvider.isStandardLibrary(qualifiedName);
        if (policyProvider.isBlacklisted(qualifiedName) || !allowed) {
            throw new IllegalArgumentException("External classifier is not allowed: " + qualifiedName);
        }
        classifiers.put(qualifiedName, externalClassifier);
    }

    @Override public void registerStructuralRelationship(UmlRelationship relationship) {
        checkNotFrozen();
        structuralRelationships.add(Objects.requireNonNull(relationship, "relationship"));
    }

    @Override public List<UmlRelationship> structuralRelationships() {
        return List.copyOf(structuralRelationships);
    }
    @Override public UnresolvedTypeRegistry unresolvedTypes() { return unresolvedTypeRegistry; }
    @Override public List<PendingResolution> getUnresolvedTypesLog() { return unresolvedTypeRegistry.getLog(); }
    @Override public Map<String, UmlClassifier> extractClassifiers() { return Map.copyOf(classifiers); }

    private void checkNotFrozen() {
        if (frozen) throw new IllegalStateException("Extraction snapshot is frozen");
    }
}
