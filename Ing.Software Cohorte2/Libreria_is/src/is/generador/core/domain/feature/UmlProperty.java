package is.generador.core.domain.feature;

import is.generador.core.domain.spec.AggregationKind;
import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import is.generador.core.domain.type.UmlType;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record UmlProperty(String name, UmlVisibility visibility, UmlType type,
        Set<UmlModifier> modifiers, Optional<String> initialValue,
        Optional<AggregationKind> aggregationKind) {

    public UmlProperty {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        name = name.trim();
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(type, "type");
        modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        initialValue = Objects.requireNonNull(initialValue, "initialValue");
        aggregationKind = Objects.requireNonNull(aggregationKind, "aggregationKind");
    }

    public boolean isStatic() {
        return modifiers.contains(UmlModifier.STATIC);
    }

    public boolean isReadOnly() {
        return modifiers.contains(UmlModifier.READ_ONLY);
    }

    public boolean isLeaf() {
        return modifiers.contains(UmlModifier.LEAF);
    }
}
