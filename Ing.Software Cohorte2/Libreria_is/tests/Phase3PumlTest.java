import is.generador.SoyLaPuertaJava;
import is.generador.core.domain.model.UmlModel;
import is.generador.infra.report.TextInventoryRenderer;
import java.nio.file.*;
import java.util.*;

public class Phase3PumlTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of("work"), "phase3-");
        Path source = Files.createDirectories(root.resolve("src"));
        Files.writeString(source.resolve("Sample.java"), """
            package sample;
            import is.generador.annotations.*;
            interface Contract { void execute(); }
            abstract class Base {}
            class Owned {}
            class Shared {}
            class Plain {}
            class Input {}
            class BodyOnly {}
            enum State { READY, DONE }
            record Value(int number) {}
            @interface Marker { String value(); }
            class Owner<T> extends Base implements Contract {
                @Composicion private Owned owned;
                @Agregacion private java.util.List<Shared> shared;
                private Plain plain;
                private int[][] numbers;
                static int counter;
                Owner(Owned part) {}
                public void execute() { BodyOnly body = new BodyOnly(); }
                public Input accept(Input value) { return value; }
                class Nested {}
            }
            """);
        var gateway = new SoyLaPuertaJava();
        var model = gateway.analyzeModel(source, root.resolve("analisis.properties"), Set.of(), Set.of());
        String puml = gateway.generarPuml(model);
        check(puml.startsWith("@startuml\n") && puml.endsWith("@enduml\n"), "Diagram delimiters");
        for (String arrow : List.of("*--", "o--", "-->", "..>", "--|>", "..|>", "+--"))
            check(puml.contains(arrow), "Missing relationship syntax " + arrow);
        check(puml.contains("abstract class") && puml.contains("interface") && puml.contains("enum")
                && puml.contains("annotation") && puml.contains("<<record>>"), "Classifier kinds");
        check(puml.contains("numbers : int[][]") && puml.contains("java.util.List<Shared>")
                && puml.contains("{static}") && !puml.contains("Parameterized("), "Member types and modifiers");
        long diagramEdges = puml.lines().filter(line -> line.matches("C\\d+ .* : .*" )).count();
        check(diagramEdges == model.diagramRelationships().size(), "Only selected edges are exported");
        check(model.relationships().size() > diagramEdges, "Full inventory survives export");
        check(new TextInventoryRenderer().render(model).contains("DEPENDE_DE"), "Console still has dependencies");
        var shuffled = new LinkedHashMap<>(model.classifiers());
        List<String> keys = new ArrayList<>(shuffled.keySet()); Collections.reverse(keys);
        Map<String, is.generador.core.domain.classifier.UmlClassifier> reverse = new LinkedHashMap<>();
        keys.forEach(key -> reverse.put(key, shuffled.get(key)));
        check(puml.equals(gateway.generarPuml(new UmlModel(reverse, model.relationships()))), "Stable generation");
        Path output = Path.of("work", "phase3-test.puml");
        gateway.guardarPuml(model, output);
        check(Files.readString(output).equals(puml), "Writer saves exact UTF-8 content");
        Files.writeString(root.resolve("analisis.properties"), "exclude.classes=sample.Shared\n");
        String configured = gateway.generarPuml(root);
        check(!configured.contains("sample.Shared"), "Facade respects saved exclusions");
        gateway.guardarPuml(root, root.resolve("nested", "configured.puml"));
        check(Files.exists(root.resolve("nested", "configured.puml")), "Writer creates parent folders");
        try {
            gateway.guardarPuml(model, source);
            throw new AssertionError("Writing over a directory should fail");
        } catch (is.generador.core.ports.UmlGenStorageException expected) { }
        Files.writeString(source.resolve("Other.java"), "package other; class Plain {}\n");
        String duplicateNames = gateway.generarPuml(root);
        check(duplicateNames.contains("sample.Plain") && duplicateNames.contains("other.Plain"), "Distinct aliases");
        check(gateway.generarPuml(new UmlModel(Map.of(), Set.of())).endsWith("@enduml\n"), "Empty model");
        System.out.println("PASS: PUML, all classifier/relationship kinds, priority, stable aliases, members, exclusions, storage");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
