package is.generador.core.domain.type;

import is.generador.core.domain.classifier.UmlClassifier;
import java.util.List;
import java.util.Objects;

/**
 * Tipo perteneciente al modelo UML, independiente del lenguaje de destino. Los
 * generadores traducen sus implementaciones a una sintaxis concreta.
 */
public interface UmlType {

    String name();

    record Primitive(String name) implements UmlType {

        public Primitive {
            name = requireText(name, "name");
        }
    }

    record TemplateParameter(String parameterName) implements UmlType {

        public TemplateParameter {
            parameterName = requireText(parameterName, "parameterName");
        }

        @Override
        public String name() {
            return parameterName;
        }
    }

    record Nullable(UmlType wrapped) implements UmlType {

        public Nullable {
            Objects.requireNonNull(wrapped, "wrapped");
        }

        @Override
        public String name() {
            return "Nullable(" + wrapped.name() + ")";
        }
    }

    record Array(UmlType elementType, int dimensions) implements UmlType {

        public Array {
            Objects.requireNonNull(elementType, "elementType");
            if (dimensions < 1) {
                throw new IllegalArgumentException("dimensions must be positive");
            }
        }

        @Override
        public String name() {
            return "Array(" + elementType.name() + ", dimensions=" + dimensions + ")";
        }
    }

    record Reference(UmlClassifier classifier) implements UmlType {

        public Reference {
            Objects.requireNonNull(classifier, "classifier");
        }

        @Override
        public String name() {
            return classifier.qualifiedName();
        }
    }

    record Unknown(String rawRepresentation) implements UmlType {

        public Unknown {
            rawRepresentation = requireText(rawRepresentation, "rawRepresentation");
        }

        @Override
        public String name() {
            return rawRepresentation;
        }
    }

    record Parameterized(UmlType base, List<UmlType> typeArguments) implements UmlType {

        public Parameterized {
            Objects.requireNonNull(base, "base");
            typeArguments = List.copyOf(Objects.requireNonNull(typeArguments, "typeArguments"));
            if (typeArguments.isEmpty()) {
                throw new IllegalArgumentException("typeArguments cannot be empty");
            }
        }

        @Override
        public String name() {
            return "Parameterized(" + base.name() + ", arguments="
                    + typeArguments.stream().map(UmlType::name).toList() + ")";
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return trimmed;
    }

}
