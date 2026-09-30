package is.generador.infra.report;

import is.generador.core.domain.model.UmlModel;
import is.generador.core.domain.relationship.UmlAssociation;
import is.generador.core.domain.relationship.UmlDependency;
import is.generador.core.domain.relationship.UmlGeneralization;
import is.generador.core.domain.relationship.UmlNesting;
import is.generador.core.domain.relationship.UmlRelationship;
import is.generador.core.ports.UmlRenderer;
import java.util.Comparator;
import java.util.Objects;

public final class TextInventoryRenderer implements UmlRenderer {
    @Override
    public String render(UmlModel model) {
        Objects.requireNonNull(model, "model");
        StringBuilder output = new StringBuilder();
        output.append(System.lineSeparator())
                .append("===============================================================")
                .append(System.lineSeparator())
                .append("                 MODELO Y RELACIONES DEL PROYECTO")
                .append(System.lineSeparator())
                .append("===============================================================")
                .append(System.lineSeparator())
                .append("Clasificadores: ").append(model.classifiers().size())
                .append(System.lineSeparator())
                .append("Relaciones: ").append(model.relationships().size())
                .append(System.lineSeparator());
        model.classifiers().values().stream()
                .sorted(Comparator.comparing(c -> c.qualifiedName()))
                .forEach(classifier -> output.append("  [")
                .append(classifier.classification()).append("] ")
                .append(classifier.qualifiedName())
                .append(" | propiedades=").append(classifier.properties().size())
                .append(" | operaciones=").append(classifier.operations().size())
                .append(System.lineSeparator()));
        output.append(System.lineSeparator()).append("RELACIONES")
                .append(System.lineSeparator());
        if (model.relationships().isEmpty()) {
            output.append("  - ninguna").append(System.lineSeparator());
        } else {
            model.relationships().stream()
                    .sorted(Comparator.comparing(this::sortKey))
                    .forEach(relationship -> output.append("  - ")
                    .append(describe(relationship))
                    .append(System.lineSeparator()));
        }
        return output.append("===============================================================")
                .append(System.lineSeparator()).toString();
    }

    private String describe(UmlRelationship relationship) {
        String kind;
        if (relationship instanceof UmlGeneralization generalization) {
            kind = generalization.isInterfaceImplementation() ? "IMPLEMENTA" : "HEREDA";
        } else if (relationship instanceof UmlNesting) {
            kind = "CONTIENE";
        } else if (relationship instanceof UmlAssociation) {
            kind = "ASOCIA";
        } else if (relationship instanceof UmlDependency) {
            kind = "DEPENDE_DE";
        } else {
            kind = relationship.getClass().getSimpleName();
        }
        return relationship.source().qualifiedName() + " --" + kind + "--> "
                + relationship.target().qualifiedName();
    }

    private String sortKey(UmlRelationship relationship) {
        return relationship.source().qualifiedName() + "|"
                + relationship.target().qualifiedName() + "|"
                + relationship.getClass().getName();
    }
}
