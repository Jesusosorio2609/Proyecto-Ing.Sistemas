package is.generador.core.domain.relationship;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.element.UmlElement;
import java.util.Objects;
import java.util.Optional;

public final class UmlGeneralization extends UmlElement implements UmlRelationship {
    private final UmlClassifier specific;
    private final UmlClassifier general;
    private final boolean interfaceImplementation;

    public UmlGeneralization(UmlClassifier specific, UmlClassifier general,
            Optional<String> stereotype, Optional<String> note, boolean isInterfaceImplementation) {
        super("generalization", Objects.requireNonNull(specific, "specific").namespace(), stereotype, note);
        this.specific = specific;
        this.general = Objects.requireNonNull(general, "general");
        this.interfaceImplementation = isInterfaceImplementation;
    }
    public UmlGeneralization(UmlClassifier specific, UmlClassifier general,
            Optional<String> stereotype, Optional<String> note) {
        this(specific, general, stereotype, note, false);
    }
    public UmlClassifier specific() { return specific; }
    public UmlClassifier general() { return general; }
    public boolean isInterfaceImplementation() { return interfaceImplementation; }
    @Override public RelationshipStrength semanticWeight() { return RelationshipStrength.STRONG; }
    @Override public UmlClassifier source() { return specific; }
    @Override public UmlClassifier target() { return general; }
}
