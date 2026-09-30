package is.generador.core.domain.relationship;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.element.UmlElement;
import java.util.Objects;
import java.util.Optional;

public final class UmlNesting extends UmlElement implements UmlRelationship {
    private final UmlClassifier outerClassifier;
    private final UmlClassifier innerClassifier;
    private final boolean isStatic;

    public UmlNesting(UmlClassifier outerClassifier, UmlClassifier innerClassifier,
            boolean isStatic, Optional<String> stereotype, Optional<String> note) {
        super("nesting", Objects.requireNonNull(outerClassifier, "outerClassifier").namespace(), stereotype, note);
        this.outerClassifier = outerClassifier;
        this.innerClassifier = Objects.requireNonNull(innerClassifier, "innerClassifier");
        this.isStatic = isStatic;
    }
    @Override public RelationshipStrength semanticWeight() { return RelationshipStrength.STRONG; }
    @Override public UmlClassifier source() { return outerClassifier; }
    @Override public UmlClassifier target() { return innerClassifier; }
    public boolean isStatic() { return isStatic; }
}
