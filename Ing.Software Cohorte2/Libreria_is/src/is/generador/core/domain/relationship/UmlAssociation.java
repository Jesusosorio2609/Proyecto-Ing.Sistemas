package is.generador.core.domain.relationship;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.element.UmlElement;
import is.generador.core.domain.spec.AggregationKind;
import java.util.Objects;
import java.util.Optional;

public final class UmlAssociation extends UmlElement implements UmlRelationship {
    private final UmlClassifier source;
    private final String sourceMultiplicity;
    private final UmlClassifier target;
    private final String targetMultiplicity;
    private final AggregationKind aggregationKind;

    public UmlAssociation(UmlClassifier source, String sourceMultiplicity, UmlClassifier target,
            String targetMultiplicity, AggregationKind aggregationKind,
            Optional<String> stereotype, Optional<String> note) {
        super("association", Objects.requireNonNull(source, "source").namespace(), stereotype, note);
        this.source = source;
        this.sourceMultiplicity = requireText(sourceMultiplicity, "sourceMultiplicity");
        this.target = Objects.requireNonNull(target, "target");
        this.targetMultiplicity = requireText(targetMultiplicity, "targetMultiplicity");
        this.aggregationKind = Objects.requireNonNull(aggregationKind, "aggregationKind");
    }
    @Override public UmlClassifier source() { return source; }
    public String sourceMultiplicity() { return sourceMultiplicity; }
    @Override public UmlClassifier target() { return target; }
    public String targetMultiplicity() { return targetMultiplicity; }
    public AggregationKind aggregationKind() { return aggregationKind; }
    @Override public RelationshipStrength semanticWeight() {
        return switch (aggregationKind) {
            case NONE -> RelationshipStrength.WEAK;
            case AGGREGATE -> RelationshipStrength.MEDIUM;
            case COMPOSITE -> RelationshipStrength.STRONG;
        };
    }
}
