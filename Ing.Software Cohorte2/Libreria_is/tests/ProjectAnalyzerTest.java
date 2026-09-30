import is.generador.ProjectAnalyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ProjectAnalyzerTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("project-metrics-");
        Files.writeString(root.resolve("Example.java"), """
            package sample;
            abstract class Example {
                int a, b;
                Example() {} Example(int a) {}
                public int getA() { return a; }
                public boolean isReady() { return true; }
                public void setA(int a) { this.a = a; }
                public int getWithArgument(int x) { return x; }
                public static int getStaticValue() { return 1; }
                public int setFluent(int x) { return x; }
                void other() { class Local {} }
                class Inner {}
                Object object = new Object() { int nested; public void run() {} };
            }
            interface Contract { int getValue(); void setValue(int x); }
            enum State { A, B; }
            record Point(int x, int y) { Point { } public int sum() { return x+y; } }
            @interface Marker { String value(); }
            """);

        var summary = ProjectAnalyzer.builder().build().analyze(root);
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("Java files", 1), Map.entry("Packages", 1),
                Map.entry("Classes", 3), Map.entry("Abstract classes", 1),
                Map.entry("Anonymous classes", 1), Map.entry("Interfaces", 1),
                Map.entry("Enums", 1), Map.entry("Records", 1),
                Map.entry("Annotation types", 1), Map.entry("Methods", 11),
                Map.entry("Fields", 4), Map.entry("Constructors", 3),
                Map.entry("Getters", 3), Map.entry("Setters", 2),
                Map.entry("Enum constants", 2), Map.entry("Record components", 2),
                Map.entry("Annotation members", 1));
        if (!summary.getTotals().equals(expected)) {
            throw new AssertionError(summary);
        }

        Path subdirectory = Files.createDirectories(root.resolve("subdirectory"));
        Files.writeString(subdirectory.resolve("Extra.java"), "package sample; class Extra {}");
        if (new ProjectAnalyzer().analyze(root).getJavaFiles() != 2) {
            throw new AssertionError("Recursive traversal failed");
        }

        try {
            new ProjectAnalyzer().analyze(root.resolve("missing"));
            throw new AssertionError("Missing directory was accepted");
        } catch (IOException expectedException) {
            // Expected.
        }
        System.out.println("OK: project metrics and recursive traversal.");
    }
}
