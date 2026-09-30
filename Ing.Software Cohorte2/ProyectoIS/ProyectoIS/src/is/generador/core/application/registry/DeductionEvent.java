package is.generador.core.application.registry;

import is.generador.core.domain.relationship.RelationshipStrength;
import is.generador.core.domain.relationship.UmlRelationship;
import java.util.Objects;

public record DeductionEvent(UmlRelationship relationship,
        RelationshipStrength weight, DeductionStatus status) {
    public DeductionEvent {
        Objects.requireNonNull(relationship, "relationship");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(status, "status");
    }
}
