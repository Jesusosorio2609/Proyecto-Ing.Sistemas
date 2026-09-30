package is.generador.infra.javaparser;

import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import is.generador.core.domain.spec.AggregationKind;

final class JavaParserAggregationMapper {
    private JavaParserAggregationMapper() {}

    static AggregationKind map(NodeWithAnnotations<?> declaration) {
        boolean aggregate = hasAnnotation(declaration, "Agregacion");
        boolean composite = hasAnnotation(declaration, "Composicion");
        if (aggregate && composite)
            throw new IllegalArgumentException("Un atributo no puede tener @Agregacion y @Composicion a la vez.");
        if (aggregate) return AggregationKind.AGGREGATE;
        if (composite) return AggregationKind.COMPOSITE;
        return AggregationKind.NONE;
    }

    private static boolean hasAnnotation(NodeWithAnnotations<?> declaration, String name) {
        return declaration.getAnnotations().stream().anyMatch(annotation ->
                annotation.getNameAsString().equals(name)
                || annotation.getNameAsString().equals("is.generador.annotations." + name));
    }
}
