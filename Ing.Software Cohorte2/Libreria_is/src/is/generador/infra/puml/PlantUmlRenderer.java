package is.generador.infra.puml;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.model.UmlModel;
import is.generador.core.domain.relationship.*;
import is.generador.core.domain.spec.UmlVisibility;
import is.generador.core.domain.type.UmlType;
import is.generador.core.ports.UmlRenderer;
import java.util.*;
import java.util.stream.Collectors;

public final class PlantUmlRenderer implements UmlRenderer {
    @Override public String render(UmlModel model) {
        Objects.requireNonNull(model, "model");
        var classifiers = model.classifiers().values().stream()
                .sorted(Comparator.comparing(UmlClassifier::qualifiedName)).toList();
        Map<String, String> aliases = new LinkedHashMap<>();
        for (int i = 0; i < classifiers.size(); i++)
            aliases.put(classifiers.get(i).qualifiedName(), "C" + i);
        StringBuilder out = new StringBuilder("@startuml\nset separator none\nskinparam classAttributeIconSize 0\n\n");
        Map<String, List<UmlClassifier>> packages = new TreeMap<>();
        for (var classifier : classifiers) {
            String namespace = classifier.namespace().toString();
            // A nested classifier belongs to its outer classifier's package.
            var outer = classifier;
            while (outer.nestingClassifier().isPresent()) outer = outer.nestingClassifier().get();
            namespace = outer.namespace().isRoot() ? "" : outer.namespace().toString();
            packages.computeIfAbsent(namespace, ignored -> new ArrayList<>()).add(classifier);
        }
        int packageId = 0;
        for (var group : packages.entrySet()) {
            boolean packaged = !group.getKey().isEmpty();
            if (packaged) out.append("package \"").append(escape(group.getKey()))
                    .append("\" as P").append(packageId++).append(" {\n");
            for (var classifier : group.getValue()) renderClassifier(out, classifier, aliases);
            if (packaged) out.append("}\n");
        }
        out.append("\n' Una relación de mayor prioridad por pareja dirigida\n");
        model.diagramRelationships().stream().sorted(Comparator.comparing(RelationshipSelection::key))
                .forEach(relationship -> renderRelationship(out, relationship, aliases));
        return out.append("@enduml\n").toString();
    }

    private void renderClassifier(StringBuilder out, UmlClassifier c, Map<String, String> aliases) {
        String keyword = switch (c.classification()) {
            case INTERFACE -> "interface";
            case ENUMERATION -> "enum";
            case ANNOTATION -> "annotation";
            default -> c.isAbstract() ? "abstract class" : "class";
        };
        String name = c.qualifiedName();
        if (!c.templateParameters().isEmpty()) name += "<" + String.join(", ", c.templateParameters()) + ">";
        out.append("  ").append(keyword).append(" \"").append(escape(name))
                .append("\" as ").append(aliases.get(c.qualifiedName()));
        if (c.classification().name().equals("RECORD")) out.append(" <<record>>");
        out.append(" {\n");
        for (var property : c.properties()) {
            out.append("    ").append(visibility(property.visibility()));
            if (property.isStatic()) out.append("{static} ");
            out.append(line(property.name())).append(" : ").append(line(type(property.type())));
            if (property.isReadOnly()) out.append(" {readOnly}");
            out.append("\n");
        }
        for (var operation : c.operations()) {
            out.append("    ").append(visibility(operation.visibility()));
            if (operation.isStatic()) out.append("{static} ");
            if (operation.isAbstract()) out.append("{abstract} ");
            out.append(line(operation.name())).append("(")
                    .append(operation.parameters().stream().map(parameter ->
                        line(parameter.name()) + " : " + line(type(parameter.type())))
                        .collect(Collectors.joining(", "))).append(")");
            if (!operation.stereotype().orElse("").equals("constructor"))
                out.append(" : ").append(line(type(operation.returnType())));
            out.append("\n");
        }
        out.append("  }\n");
    }

    private void renderRelationship(StringBuilder out, UmlRelationship r, Map<String, String> aliases) {
        String source = aliases.get(r.source().qualifiedName());
        String target = aliases.get(r.target().qualifiedName());
        if (source == null || target == null) throw new IllegalArgumentException("Relación con clasificador fuera del modelo");
        out.append(source);
        if (r instanceof UmlAssociation association) {
            String arrow = switch (association.aggregationKind()) {
                case COMPOSITE -> "*--";
                case AGGREGATE -> "o--";
                case NONE -> "-->";
            };
            out.append(" \"").append(escape(association.sourceMultiplicity())).append("\" ")
                    .append(arrow).append(" \"").append(escape(association.targetMultiplicity()))
                    .append("\" ").append(target);
        } else if (r instanceof UmlGeneralization generalization) {
            out.append(generalization.isInterfaceImplementation() ? " ..|> " : " --|> ").append(target);
        } else if (r instanceof UmlNesting) {
            out.append(" +-- ").append(target);
        } else {
            out.append(" ..> ").append(target);
        }
        out.append(" : ").append(RelationshipSelection.kind(r)).append("\n");
    }

    private String visibility(UmlVisibility visibility) {
        return switch (visibility) { case PUBLIC -> "+"; case PRIVATE -> "-"; case PROTECTED -> "#"; case PACKAGE -> "~"; };
    }

    private String type(UmlType value) {
        if (value instanceof UmlType.Array array) return type(array.elementType()) + "[]".repeat(array.dimensions());
        if (value instanceof UmlType.Parameterized generic)
            return type(generic.base()) + "<" + generic.typeArguments().stream().map(this::type)
                    .collect(Collectors.joining(", ")) + ">";
        if (value instanceof UmlType.Nullable nullable) return type(nullable.wrapped()) + "?";
        return value.name();
    }
    private String line(String value) { return value.replace('\r', ' ').replace('\n', ' '); }
    private String escape(String value) { return line(value).replace("\\", "\\\\").replace("\"", "\\\""); }
}
