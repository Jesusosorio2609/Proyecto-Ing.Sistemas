package is.generador.infra.javaparser;

import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import is.generador.core.domain.type.UmlType;
import java.util.Objects;
import java.util.Set;

final class JavaParserTypeMapper {

    UmlType map(Type type, Set<String> templateParameters) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(templateParameters, "templateParameters");

        if (type.isPrimitiveType() || type.isVoidType()) {
            return new UmlType.Primitive(type.asString());
        }
        if (type.isArrayType()) {
            int dimensions = 0;
            Type elementType = type;
            while (elementType.isArrayType()) {
                dimensions++;
                elementType = elementType.asArrayType().getComponentType();
            }
            return new UmlType.Array(map(elementType, templateParameters), dimensions);
        }
        if (type.isClassOrInterfaceType()) {
            return mapClassOrInterface(type.asClassOrInterfaceType(), templateParameters);
        }
        return new UmlType.Unknown(type.asString());
    }

    private UmlType mapClassOrInterface(ClassOrInterfaceType type,
            Set<String> templateParameters) {
        String rawName = type.getNameWithScope();
        UmlType base = !type.getScope().isPresent() && templateParameters.contains(rawName)
                ? new UmlType.TemplateParameter(rawName)
                : new UmlType.Unknown(rawName);

        return type.getTypeArguments()
                .filter(arguments -> !arguments.isEmpty())
                .<UmlType>map(arguments -> new UmlType.Parameterized(
                base,
                arguments.stream()
                        .map(argument -> map(argument, templateParameters))
                        .toList()))
                .orElse(base);
    }
}
