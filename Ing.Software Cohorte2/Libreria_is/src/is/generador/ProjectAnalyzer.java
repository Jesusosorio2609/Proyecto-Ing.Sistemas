package is.generador;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

/** Analyzes source declarations without loading or running project classes. */
public final class ProjectAnalyzer {
    private final Set<String> excludedPackages;
    private final Set<String> excludedClasses;

    /** Creates an analyzer without exclusions. */
    public ProjectAnalyzer() {
        this(new Builder());
    }

    private ProjectAnalyzer(Builder builder) {
        excludedPackages = Collections.unmodifiableSet(new LinkedHashSet<>(builder.packages));
        excludedClasses = Collections.unmodifiableSet(new LinkedHashSet<>(builder.classes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Set<String> packages = new LinkedHashSet<>();
        private final Set<String> classes = new LinkedHashSet<>();

        private Builder() {}

        /** Excludes the specified package and all its subpackages. */
        public Builder excludePackage(String packageName) {
            packages.add(validateJavaName(packageName));
            return this;
        }

        /**
         * Excludes a type by simple or qualified name, such as sample.Outer.Inner.
         * This method also supports interfaces, enums, records, and annotation types.
         */
        public Builder excludeClass(String className) {
            classes.add(validateJavaName(className));
            return this;
        }

        public ProjectAnalyzer build() {
            return new ProjectAnalyzer(this);
        }

        private static String validateJavaName(String name) {
            if (name == null || !SourceVersion.isName(name.trim())) {
                throw new IllegalArgumentException("Invalid Java name: " + name);
            }
            return name.trim();
        }
    }

    public ProjectSummary analyze(Path sourceDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException("Source directory does not exist: " + sourceDirectory.toAbsolutePath());
        }

        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        ProjectSummary summary = new ProjectSummary();

        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            Iterator<Path> files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .iterator();

            while (files.hasNext()) {
                Path file = files.next();
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (!result.isSuccessful() || result.getResult().isEmpty()) {
                    throw new IOException("Could not analyze " + file + ": " + result.getProblems());
                }

                CompilationUnit unit = result.getResult().get();
                String packageName = unit.getPackageDeclaration()
                        .map(declaration -> declaration.getNameAsString())
                        .orElse("");

                if (isPackageExcluded(packageName)) {
                    continue;
                }
                if (!unit.getTypes().isEmpty()
                        && unit.getTypes().stream().noneMatch(type -> isIncluded(type, packageName))) {
                    continue;
                }

                summary.add("Java files", 1);
                unit.getPackageDeclaration()
                        .ifPresent(declaration -> summary.addPackage(declaration.getNameAsString()));

                for (ClassOrInterfaceDeclaration type : included(unit, ClassOrInterfaceDeclaration.class)) {
                    summary.add(type.isInterface() ? "Interfaces" : "Classes", 1);
                    if (!type.isInterface() && type.isAbstract()) {
                        summary.add("Abstract classes", 1);
                    }
                }

                summary.add("Enums", included(unit, EnumDeclaration.class).size());
                summary.add("Enum constants", included(unit, EnumConstantDeclaration.class).size());
                summary.add("Records", included(unit, RecordDeclaration.class).size());
                for (RecordDeclaration type : included(unit, RecordDeclaration.class)) {
                    summary.add("Record components", type.getParameters().size());
                }

                summary.add("Annotation types", included(unit, AnnotationDeclaration.class).size());
                summary.add("Annotation members", included(unit, AnnotationMemberDeclaration.class).size());
                summary.add("Anonymous classes", (int) included(unit, ObjectCreationExpr.class).stream()
                        .filter(expression -> expression.getAnonymousClassBody().isPresent())
                        .count());

                for (FieldDeclaration field : included(unit, FieldDeclaration.class)) {
                    summary.add("Fields", field.getVariables().size());
                }

                summary.add("Constructors", included(unit, ConstructorDeclaration.class).size()
                        + included(unit, CompactConstructorDeclaration.class).size());

                for (MethodDeclaration method : included(unit, MethodDeclaration.class)) {
                    summary.add("Methods", 1);
                    if (isGetter(method)) {
                        summary.add("Getters", 1);
                    }
                    if (isSetter(method)) {
                        summary.add("Setters", 1);
                    }
                }
            }
        }
        return summary;
    }

    private boolean isPackageExcluded(String packageName) {
        return excludedPackages.stream()
                .anyMatch(excluded -> packageName.equals(excluded)
                        || packageName.startsWith(excluded + "."));
    }

    private boolean isIncluded(Node node, String packageName) {
        for (Node current = node; current != null; current = current.getParentNode().orElse(null)) {
            if (current instanceof TypeDeclaration<?> type) {
                List<String> names = new ArrayList<>();
                for (Node parent = type; parent != null; parent = parent.getParentNode().orElse(null)) {
                    if (parent instanceof TypeDeclaration<?> enclosingType) {
                        names.add(enclosingType.getNameAsString());
                    }
                }
                Collections.reverse(names);
                String qualifiedName = (packageName.isEmpty() ? "" : packageName + ".")
                        + String.join(".", names);
                if (excludedClasses.contains(type.getNameAsString())
                        || excludedClasses.contains(qualifiedName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private <T extends Node> List<T> included(CompilationUnit unit, Class<T> nodeType) {
        String packageName = unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");
        return unit.findAll(nodeType).stream()
                .filter(node -> isIncluded(node, packageName))
                .toList();
    }

    // Uses JavaBeans names and signatures; method bodies are not interpreted.
    private boolean isGetter(MethodDeclaration method) {
        if (method.isStatic() || !isPublic(method)
                || !method.getParameters().isEmpty() || method.getType().isVoidType()) {
            return false;
        }
        String name = method.getNameAsString();
        return (name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2
                && method.getType().asString().equals("boolean"));
    }

    private boolean isSetter(MethodDeclaration method) {
        String name = method.getNameAsString();
        return !method.isStatic() && isPublic(method)
                && name.startsWith("set") && name.length() > 3
                && method.getParameters().size() == 1 && method.getType().isVoidType();
    }

    private boolean isPublic(MethodDeclaration method) {
        return method.isPublic() || (!method.isPrivate() && method.getParentNode()
                .filter(parent -> parent instanceof ClassOrInterfaceDeclaration)
                .map(parent -> ((ClassOrInterfaceDeclaration) parent).isInterface())
                .orElse(false));
    }
}
