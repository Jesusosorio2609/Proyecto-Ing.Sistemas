package is.generador.infra.javaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class JavaParserSpecMapper {

    private JavaParserSpecMapper() {
    }

    static UmlVisibility toVisibility(NodeWithAccessModifiers<?> node) {
        Objects.requireNonNull(node, "node");
        if (node.hasModifier(Modifier.Keyword.PUBLIC)) {
            return UmlVisibility.PUBLIC;
        }
        if (node.hasModifier(Modifier.Keyword.PROTECTED)) {
            return UmlVisibility.PROTECTED;
        }
        if (node.hasModifier(Modifier.Keyword.PRIVATE)) {
            return UmlVisibility.PRIVATE;
        }
        return UmlVisibility.PACKAGE;
    }

    static Set<UmlModifier> toClassifierModifiers(NodeWithModifiers<?> node) {
        Objects.requireNonNull(node, "node");
        EnumSet<UmlModifier> result = EnumSet.noneOf(UmlModifier.class);
        addCommonModifiers(node, result);
        if (node.hasModifier(Modifier.Keyword.FINAL)) {
            result.add(UmlModifier.LEAF);
        }
        return Set.copyOf(result);
    }

    static Set<UmlModifier> toPropertyModifiers(NodeWithModifiers<?> node) {
        Objects.requireNonNull(node, "node");
        EnumSet<UmlModifier> result = EnumSet.noneOf(UmlModifier.class);
        if (node.hasModifier(Modifier.Keyword.STATIC)) {
            result.add(UmlModifier.STATIC);
        }
        if (node.hasModifier(Modifier.Keyword.FINAL)) {
            result.add(UmlModifier.READ_ONLY);
        }
        return Set.copyOf(result);
    }

    static Set<UmlModifier> toOperationModifiers(NodeWithModifiers<?> node) {
        Objects.requireNonNull(node, "node");
        EnumSet<UmlModifier> result = EnumSet.noneOf(UmlModifier.class);
        addCommonModifiers(node, result);
        if (node.hasModifier(Modifier.Keyword.FINAL)) {
            result.add(UmlModifier.LEAF);
        }
        return Set.copyOf(result);
    }

    private static void addCommonModifiers(NodeWithModifiers<?> node,
            EnumSet<UmlModifier> result) {
        if (node.hasModifier(Modifier.Keyword.ABSTRACT)) {
            result.add(UmlModifier.ABSTRACT);
        }
        if (node.hasModifier(Modifier.Keyword.STATIC)) {
            result.add(UmlModifier.STATIC);
        }
    }
}
