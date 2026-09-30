package is.generador.core.domain.relationship;

import is.generador.core.domain.classifier.UmlClassifier;

public interface UmlRelationship {
    UmlClassifier source();
    UmlClassifier target();
    RelationshipStrength semanticWeight();
}
