import is.generador.ProjectAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExclusionBuilderTest {
    private static void verify(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("exclusions-");
        Files.writeString(root.resolve("A.java"), """
            package sample;
            class A {
                int x; A() {} public int getX() { return x; }
                public void setX(int x) { this.x = x; }
                class Inner { int y; void run() {} }
            }
            class B { int z; B() {} void other() {} }
            """);
        Files.writeString(root.resolve("Other.java"), "package other; class A { int x; }");
        Files.writeString(root.resolve("Sub.java"), "package sample.sub; record Sub(int x) { Sub {} }");
        Files.writeString(root.resolve("Similar.java"), "package sample2; interface Similar { void run(); }");

        var all = new ProjectAnalyzer().analyze(root);
        verify(all.getJavaFiles() == 4 && all.getClasses() == 4, "Baseline counts");

        var withoutA = ProjectAnalyzer.builder().excludeClass("sample.A").build().analyze(root);
        verify(withoutA.getClasses() == 2 && withoutA.getJavaFiles() == 4,
                "Another type in a shared file must remain");
        verify(withoutA.getFields() == 2 && withoutA.getMethods() == 2
                && withoutA.getConstructors() == 2 && withoutA.getGetters() == 0
                && withoutA.getSetters() == 0, "Members and nested types must be excluded");

        var bySimpleName = ProjectAnalyzer.builder().excludeClass("A").build().analyze(root);
        verify(bySimpleName.getClasses() == 1 && bySimpleName.getJavaFiles() == 3,
                "Simple names must match all packages");

        var withoutPackage = ProjectAnalyzer.builder().excludePackage("sample").build().analyze(root);
        verify(withoutPackage.getJavaFiles() == 2 && withoutPackage.getRecords() == 0
                && withoutPackage.getInterfaces() == 1, "Package and subpackage exclusion");

        var builder = ProjectAnalyzer.builder().excludePackage(" sample ").excludeClass("other.A");
        var firstAnalyzer = builder.build();
        builder.excludePackage("sample2");
        verify(firstAnalyzer.analyze(root).getInterfaces() == 1, "Built analyzer must be immutable");
        verify(builder.build().analyze(root).getTotals().values().stream().allMatch(value -> value == 0),
                "All declarations must be excluded");

        System.out.println("OK: builder package and class exclusions.");
    }
}
