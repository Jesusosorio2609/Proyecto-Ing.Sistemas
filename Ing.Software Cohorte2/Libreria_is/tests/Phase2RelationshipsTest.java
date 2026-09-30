import is.generador.SoyLaPuertaJava;
import is.generador.core.domain.model.UmlModel;
import is.generador.core.domain.relationship.*;
import is.generador.core.domain.spec.AggregationKind;
import is.generador.infra.report.TextInventoryRenderer;
import java.nio.file.*;
import java.util.*;

public class Phase2RelationshipsTest {
    public static void main(String[] args) throws Exception {
        Path source = Files.createTempDirectory("phase2-sources-");
        Files.writeString(source.resolve("Sample.java"), """
                package sample;
                class Target {}
                class BodyOnly {}
                class Owner {
                    Target field;
                    Target other;
                    Target method(Target argument) {
                        BodyOnly local = new BodyOnly();
                        return argument;
                    }
                }
                """);
        var model = new SoyLaPuertaJava().analyzeModel(source,
                source.resolve("missing.properties"), Set.of(), Set.of());
        var relations = model.relationships();
        check(relations.size() == 2, "Retain association and dependency; deduplicate repeated signatures");
        check(relations.stream().noneMatch(r -> r.target().name().equals("BodyOnly")),
                "Do not inspect method bodies");
        check(model.diagramRelationships().size() == 1
                && model.diagramRelationships().get(0) instanceof UmlAssociation,
                "Association outranks dependency in diagram only");
        String report = new TextInventoryRenderer().render(model);
        check(report.contains("ASOCIACION") && report.contains("DEPENDE_DE"),
                "Console lists all relation kinds");
        var owner = model.findClassifier("sample.Owner").orElseThrow();
        var target = model.findClassifier("sample.Target").orElseThrow();
        var aggregate = new UmlAssociation(owner, "1", target, "1", AggregationKind.AGGREGATE,
                Optional.empty(), Optional.empty());
        var composite = new UmlAssociation(owner, "1", target, "1", AggregationKind.COMPOSITE,
                Optional.empty(), Optional.empty());
        List<UmlRelationship> all = new ArrayList<>(relations);
        all.add(aggregate); all.add(composite);
        all.add(new UmlDependency(target, owner, Optional.empty(), Optional.empty()));
        var richer = new UmlModel(model.classifiers(), Set.copyOf(all));
        check(richer.relationships().size() == 5, "Keep all kinds and reverse direction");
        check(richer.diagramRelationships().size() == 2
                && richer.diagramRelationships().contains(composite),
                "Composition wins; reverse direction remains independent");
        String detailed = new TextInventoryRenderer().render(richer);
        check(detailed.contains("AGREGACION") && detailed.contains("COMPOSICION"),
                "Report names aggregation and composition");
        annotationChecks();
        System.out.println("PASS: full inventory, diagram priorities, directed pairs, deduplication, no body analysis");
    }

    private static void annotationChecks() throws Exception {
        Path source = Files.createTempDirectory("phase2-annotations-");
        Files.writeString(source.resolve("Annotated.java"), """
                package sample;
                import is.generador.annotations.Agregacion;
                import is.generador.annotations.Composicion;
                import java.util.List;
                class Part {}
                class Shared {}
                class Plain {}
                class Problem extends Exception {}
                class T {}
                class Recursive { Recursive next; }
                class Owner<T> {
                    @Composicion Part[] parts;
                    @Agregacion List<Shared> shared;
                    Plain plain = new Plain();
                    T generic;
                    Owner(Plain input) throws Problem {}
                    Part operation(Part value) throws Problem { return value; }
                }
                record Container(@is.generador.annotations.Composicion Part part) {}
                """);
        var gateway = new SoyLaPuertaJava();
        var model = gateway.analyzeModel(source, source.resolve("missing.properties"), Set.of(), Set.of());
        check(has(model, "Owner", "Part", "COMPOSICION"), "Annotated composition");
        check(has(model, "Owner", "Part", "DEPENDENCIA"), "Composition and dependency both retained");
        check(has(model, "Owner", "Shared", "AGREGACION"), "Annotated generic aggregation");
        check(has(model, "Owner", "Plain", "ASOCIACION"), "Initializer is not interpreted as ownership");
        check(has(model, "Owner", "Plain", "DEPENDENCIA"), "Constructor parameter dependency");
        check(has(model, "Owner", "Problem", "DEPENDENCIA"), "Throws signature dependency");
        check(has(model, "Container", "Part", "COMPOSICION"), "Record component annotation");
        check(has(model, "Recursive", "Recursive", "ASOCIACION"), "Recursive association");
        check(model.relationships().stream().noneMatch(r -> r.target().name().equals("T")),
                "Type parameter is not a concrete classifier");
        check(model.relationships().stream().filter(r -> r instanceof UmlAssociation
                && (r.target().name().equals("Shared") || r.source().name().equals("Owner")
                    && r.target().name().equals("Part")))
                .allMatch(r -> ((UmlAssociation) r).targetMultiplicity().equals("0..*")),
                "Arrays and collections have many-valued target multiplicity");
        var filtered = gateway.analyzeModel(source, source.resolve("missing.properties"), Set.of(), Set.of("Part"));
        check(filtered.relationships().stream().noneMatch(r -> r.target().name().equals("Part")),
                "Excluded classifier cannot participate in relations");
        Files.writeString(source.resolve("Invalid.java"), """
                package sample;
                class Invalid {
                    @is.generador.annotations.Agregacion
                    @is.generador.annotations.Composicion Part part;
                }
                """);
        try {
            gateway.analyzeModel(source, source.resolve("missing.properties"), Set.of(), Set.of());
            throw new AssertionError("Contradictory annotations must fail visibly");
        } catch (java.io.IOException expected) {
            check(expected.getMessage().contains("a la vez"), "Actionable annotation error");
        }
    }

    private static boolean has(UmlModel model, String source, String target, String kind) {
        return model.relationships().stream().anyMatch(r -> r.source().name().equals(source)
                && r.target().name().equals(target) && RelationshipSelection.kind(r).equals(kind));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
