package is.generador.core.domain.feature;

import is.generador.core.domain.type.UmlType;
import java.util.Objects;

public record UmlParameter(String name, UmlType type) {

    public UmlParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        name = name.trim();
    }
}
