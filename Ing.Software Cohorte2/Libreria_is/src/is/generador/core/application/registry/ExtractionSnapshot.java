package is.generador.core.application.registry;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.context.UnresolvedTypeRegistry;
import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.classifier.UmlConcreteClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExtractionSnapshot {
    Map<String, UmlClassifier> availableClassifiers();
    Optional<UmlClassifier> findClassifier(String qualifiedName);
    void registerClassifier(UmlClassifier classifier);
    void replaceClassifier(String fqn, UmlConcreteClassifier newClassifier);
    void registerExternalClassifier(String qualifiedName, UmlClassifier externalClassifier);
    void registerStructuralRelationship(UmlRelationship relationship);
    List<UmlRelationship> structuralRelationships();
    UnresolvedTypeRegistry unresolvedTypes();
    List<PendingResolution> getUnresolvedTypesLog();
    void freeze();
    Map<String, UmlClassifier> extractClassifiers();
}
