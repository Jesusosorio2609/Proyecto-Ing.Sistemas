package is.generador.core.application.registry;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResolutionRegistry {
    Map<String, UmlClassifier> availableClassifiers();
    Optional<UmlClassifier> findClassifier(String qualifiedName);
    Collection<UmlRelationship> resolvedRelationships();
    List<DeductionEvent> deductionEvents();
    void registerRelationship(UmlRelationship relationship);
}
