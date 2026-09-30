package is.generador.core.domain.relationship;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.element.UmlElement;
import java.util.Objects;
import java.util.Optional;

public final class UmlDependency extends UmlElement implements UmlRelationship {
    private final UmlClassifier client;
    private final UmlClassifier supplier;

    public UmlDependency(UmlClassifier client, UmlClassifier supplier,
            Optional<String> stereotype, Optional<String> note) {
        super("dependency", Objects.requireNonNull(client, "client").namespace(), stereotype, note);
        this.client = client;
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }
    public UmlClassifier client() { return client; }
    public UmlClassifier supplier() { return supplier; }
    @Override public RelationshipStrength semanticWeight() { return RelationshipStrength.WEAK; }
    @Override public UmlClassifier source() { return client; }
    @Override public UmlClassifier target() { return supplier; }
    @Override public String toString() { return client.qualifiedName() + " -> " + supplier.qualifiedName(); }
}
