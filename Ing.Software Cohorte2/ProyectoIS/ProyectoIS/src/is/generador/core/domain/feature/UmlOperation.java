package is.generador.core.domain.feature;

import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import is.generador.core.domain.type.UmlType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record UmlOperation(String name, UmlVisibility visibility, UmlType returnType,
        List<UmlParameter> parameters, Set<UmlModifier> modifiers, Optional<String> stereotype) {
    public UmlOperation {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(returnType, "returnType");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        stereotype = Objects.requireNonNull(stereotype, "stereotype");
    }
    public boolean isLeaf() { return modifiers.contains(UmlModifier.LEAF); }
    public boolean isStatic() { return modifiers.contains(UmlModifier.STATIC); }
    public boolean isAbstract() { return modifiers.contains(UmlModifier.ABSTRACT); }
}
