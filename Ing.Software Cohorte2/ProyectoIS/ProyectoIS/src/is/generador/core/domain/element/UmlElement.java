package is.generador.core.domain.element;

import java.util.Objects;
import java.util.Optional;

public abstract class UmlElement {
    private final String name;
    private final UmlNamespace namespace;
    private final String qualifiedName;
    private final Optional<String> stereotype;
    private final Optional<String> note;

    protected UmlElement(String name, UmlNamespace namespace, Optional<String> stereotype, Optional<String> note) {
        this.name = requireText(name, "name");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.qualifiedName = namespace.qualifiedNameOf(this.name);
        this.stereotype = normalized(stereotype, "stereotype");
        this.note = normalized(note, "note");
    }

    public final String name() { return name; }
    public final UmlNamespace namespace() { return namespace; }
    public final String qualifiedName() { return qualifiedName; }
    public final Optional<String> stereotype() { return stereotype; }
    public final Optional<String> note() { return note; }

    protected static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        var trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return trimmed;
    }

    private static Optional<String> normalized(Optional<String> value, String field) {
        Objects.requireNonNull(value, field);
        return value.map(text -> requireText(text, field));
    }
}
