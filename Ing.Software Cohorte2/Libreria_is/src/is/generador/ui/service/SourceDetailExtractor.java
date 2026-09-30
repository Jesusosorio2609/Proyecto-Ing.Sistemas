package is.generador.ui.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import is.generador.ui.model.ClassifierFileDetails;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Extracts the source-only sections used by the detailed inventory report. */
final class SourceDetailExtractor {
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));

    Map<String, ClassifierFileDetails> extract(Path sourceDirectory,
            Set<String> blacklistedLibraries) throws IOException {
        List<SourceUnit> units = readUnits(sourceDirectory);
        Set<String> internalTypes = new LinkedHashSet<>();
        Set<String> internalPackages = new LinkedHashSet<>();
        for (SourceUnit unit : units) {
            internalPackages.add(unit.packageName());
            for (TypeDeclaration<?> type : unit.unit().getTypes()) {
                collectTypeNames(type, unit.packageName(), internalTypes);
            }
        }
        Map<String, ClassifierFileDetails> result = new LinkedHashMap<>();
        for (SourceUnit unit : units) {
            ImportGroups imports = classifyImports(unit.unit(), internalTypes,
                    internalPackages, blacklistedLibraries);
            for (TypeDeclaration<?> type : unit.unit().getTypes()) {
                collectDetails(type, unit.packageName(), unit.path(), imports, result);
            }
        }
        return result;
    }

    private List<SourceUnit> readUnits(Path sourceDirectory) throws IOException {
        List<SourceUnit> units = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(value -> value.toString().endsWith(".java")).sorted().toList()) {
                CompilationUnit unit = parser.parse(path).getResult().orElseThrow(
                        () -> new IOException("Could not parse " + path));
                String packageName = unit.getPackageDeclaration()
                        .map(value -> value.getNameAsString()).orElse("");
                units.add(new SourceUnit(path, packageName, unit));
            }
        }
        return units;
    }

    private void collectTypeNames(TypeDeclaration<?> type, String packageName,
            Set<String> names) {
        names.add(qualifiedName(packageName, type.getNameAsString()));
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collectTypeNames(nested, packageName, names);
            }
        }
    }

    private void collectDetails(TypeDeclaration<?> type, String packageName, Path path,
            ImportGroups imports, Map<String, ClassifierFileDetails> destination) {
        List<String> properties = type.getFields().stream()
                .flatMap(field -> field.getVariables().stream()
                        .map(variable -> field.getElementType() + " " + variable.getName()))
                .toList();
        List<String> methods = type.getMethods().stream().map(this::signature).toList();
        List<String> nestedTypes = type.getMembers().stream()
                .filter(TypeDeclaration.class::isInstance)
                .map(member -> ((TypeDeclaration<?>) member).getNameAsString()).toList();
        ClassifierFileDetails details = new ClassifierFileDetails(path,
                imports.internal(), imports.javaNative(), imports.external(),
                type instanceof RecordDeclaration record
                        ? record.getParameters().stream()
                                .map(value -> value.getType() + " " + value.getName()).toList()
                        : List.of(),
                type.isEnumDeclaration()
                        ? type.asEnumDeclaration().getEntries().stream()
                                .map(value -> value.getNameAsString()).toList() : List.of(),
                type instanceof AnnotationDeclaration annotation
                        ? annotation.getMembers().stream()
                                .map(member -> annotationSignature(
                                (AnnotationMemberDeclaration) member)).toList() : List.of(),
                properties,
                type.getConstructors().stream().map(value ->
                        value.getDeclarationAsString(false, false, false)).toList(),
                type.getMembers().stream().filter(CompactConstructorDeclaration.class::isInstance)
                        .map(value -> ((CompactConstructorDeclaration) value)
                        .getNameAsString() + "(...)").toList(),
                methods,
                type.getMethods().stream().filter(this::isGetter).map(this::signature).toList(),
                type.getMethods().stream().filter(this::isSetter).map(this::signature).toList(),
                nestedTypes);
        destination.put(qualifiedName(packageName, type.getNameAsString()), details);
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collectDetails(nested, packageName, path, imports, destination);
            }
        }
    }

    private ImportGroups classifyImports(CompilationUnit unit, Set<String> internalTypes,
            Set<String> internalPackages, Set<String> blocked) {
        List<String> internal = new ArrayList<>();
        List<String> javaNative = new ArrayList<>();
        List<String> external = new ArrayList<>();
        for (ImportDeclaration declaration : unit.getImports()) {
            if (declaration.isAsterisk() || !isUsed(unit, declaration)) continue;
            String name = declaration.getNameAsString();
            if (matchesPolicy(name, blocked)) continue;
            if (internalTypes.contains(name)
                    || internalTypes.stream().anyMatch(value -> value.startsWith(name + "."))
                    || internalPackages.stream().anyMatch(value -> name.startsWith(value + "."))) {
                internal.add(name);
            } else if (name.startsWith("java.") || name.startsWith("javax.")) {
                javaNative.add(name);
            } else {
                external.add(name);
            }
        }
        return new ImportGroups(internal, javaNative, external);
    }

    private boolean isUsed(CompilationUnit unit, ImportDeclaration declaration) {
        String simpleName = declaration.getName().getIdentifier();
        return unit.getTypes().stream().flatMap(type -> type.findAll(SimpleName.class).stream())
                .anyMatch(value -> value.getIdentifier().equals(simpleName));
    }

    private boolean matchesPolicy(String importName, Set<String> policy) {
        return policy.stream().anyMatch(value -> importName.equals(value)
                || importName.endsWith("." + value));
    }

    private String signature(MethodDeclaration method) {
        return method.getDeclarationAsString(false, false, false);
    }

    private String annotationSignature(AnnotationMemberDeclaration member) {
        return member.getType() + " " + member.getName();
    }

    private boolean isGetter(MethodDeclaration method) {
        String name = method.getNameAsString();
        return !method.isStatic() && method.isPublic() && method.getParameters().isEmpty()
                && !method.getType().isVoidType()
                && ((name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2
                && method.getType().asString().equals("boolean")));
    }

    private boolean isSetter(MethodDeclaration method) {
        String name = method.getNameAsString();
        return !method.isStatic() && method.isPublic() && name.startsWith("set")
                && name.length() > 3 && method.getParameters().size() == 1
                && method.getType().isVoidType();
    }

    private String qualifiedName(String packageName, String typeName) {
        return packageName.isBlank() ? typeName : packageName + "." + typeName;
    }

    private record SourceUnit(Path path, String packageName, CompilationUnit unit) {}
    private record ImportGroups(List<String> internal, List<String> javaNative,
            List<String> external) {}
}
