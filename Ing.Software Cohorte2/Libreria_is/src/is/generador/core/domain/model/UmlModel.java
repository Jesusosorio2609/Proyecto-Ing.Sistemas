package is.generador.core.domain.model;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import is.generador.core.domain.relationship.RelationshipSelection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record UmlModel(Map<String, UmlClassifier> classifiers, Set<UmlRelationship> relationships) {
    public UmlModel {
        classifiers = Map.copyOf(Objects.requireNonNull(classifiers, "classifiers"));
        relationships = Set.copyOf(Objects.requireNonNull(relationships, "relationships"));
    }
    public Optional<UmlClassifier> findClassifier(String qualifiedName) {
        return Optional.ofNullable(classifiers.get(Objects.requireNonNull(qualifiedName, "qualifiedName")));
    }

    /** Projection for the future PUML renderer; relationships() retains all kinds. */
    public List<UmlRelationship> diagramRelationships() {
        return RelationshipSelection.strongest(relationships);
    }
}
